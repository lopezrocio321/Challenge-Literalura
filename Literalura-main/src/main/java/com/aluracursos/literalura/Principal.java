package com.aluracursos.literalura;

import com.aluracursos.literalura.model.*;
import com.aluracursos.literalura.repository.AutorRepository;
import com.aluracursos.literalura.repository.LibroRepository;
import com.aluracursos.literalura.service.ConsumoAPI;
import com.aluracursos.literalura.service.ConvierteDatos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Principal {

    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private String mensajeOpcionInvalida = "Opción no válida. Por favor, inténtelo de nuevo.";
    private List<Libro> libros;
    private List<Autor> autores;


    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private AutorRepository autorRepository;

    public Principal(LibroRepository libroRepository, AutorRepository autorRepository){
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    ---------------MENÚ----------------
                    Por favor, seleccione el número de la opción que desea ejecutar:
                    1 - Buscar un libro por su título
                    2 - Mostrar la lista completa de libros registrados
                    3 - Ver la lista de autores registrados
                    4 - Consultar autores vivos en un año específico
                    5 - Filtrar libros por idioma
                    6 - Ver el Top 10 de libros más descargados
                    7 - Revisar estadísticas de los libros registrados
                    
                    0 - Finalizar sesión
                    ------------------------------------
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    guardarLibro();
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    listarAutoresVivosPorAnio();
                    break;
                case 5:
                    listarLibrosPorIdioma();
                    break;
                case 6:
                    topLibrosMasDescargados();
                    break;
                case 7:
                    mostrarEstadisticas();
                    break;
                case 0:
                    System.out.println("Saliendo de la aplicación....");
                    break;
                default:
                    System.out.println(mensajeOpcionInvalida);
            }
        }
    }

    public DatosLibro getDatosLibro() {
        System.out.println("Indica el título del libro que estás buscando: ");
        var tituloBuscado = teclado.nextLine();
        try {
            var json = consumoApi.obtenerDatos(tituloBuscado.replace(" ", "+"));
            var datos = conversor.obtenerDatos(json, DatosLibro.class);
            return datos;
        } catch (Exception e) {
            System.out.println(mensajeOpcionInvalida);
        }
        return null;
    }

    private void guardarLibro() {
        DatosLibro datosLibro = getDatosLibro();
        if (datosLibro != null) {
        Optional<Autor> nombreAutor = autorRepository.findByNombre(datosLibro.autor().get(0).nombre());

            Libro libro = new Libro();
            libro.setTitulo(datosLibro.titulo());
            libro.setIdioma(Idioma.fromString(datosLibro.idioma().get(0)));
            libro.setNumeroDescargas(datosLibro.numeroDescargas());

            if (nombreAutor.isPresent()) {
                Autor autorExistente = nombreAutor.get();
                libro.setAutor(autorExistente);
            } else {
                Autor nuevoAutor = new Autor(datosLibro.autor().get(0));
                nuevoAutor = autorRepository.save(nuevoAutor);
                libro.setAutor(nuevoAutor);
            }
            try {
                Optional<Libro> libroExistente = libroRepository.findByTituloContainingIgnoreCase(datosLibro.titulo());
                if (libroExistente.isPresent()) {
                    System.out.println("Este libro ya se encuentra registrado");
                    return;
                }
                libroRepository.save(libro);
                System.out.println("El libro se registró con éxito");
                System.out.println(libro);
            } catch (Exception e) {
                System.out.println("El libro fue registrado previamente.");
            }
        } else {
            System.out.println(mensajeOpcionInvalida);
        }
    }

    private void listarLibrosRegistrados(){
        System.out.println("---Lista de libros registrados---");
        libros =libroRepository.findAll();
        if (libros.isEmpty()){
            System.out.println("Aún no se han registrado libros en la base de datos");
        } else {
            libros.stream()
                    .sorted(Comparator.comparing(Libro::getTitulo))
                    .forEach(System.out::println);
        }

    }

    private void listarAutoresRegistrados(){
        System.out.println("---Listado de autores registrados---");
        autores = autorRepository.findAll();
        if (autores.isEmpty()){
            System.out.println("Aún no se han registrado autores en la base de datos.");
        } else {
            for (int i = 0; i < autores.size(); i++){
                List<Libro> librosPorAutor = libroRepository.findLibrosByAutorId(autores.get(i).getId());
                System.out.println("\n" + autores.get(i).toString());
                System.out.println("Libros registrados: ");
                librosPorAutor.forEach(l -> System.out.println("- " + l.getTitulo()));
            }
        }
    }

    private void listarAutoresVivosPorAnio(){
        System.out.println("Por favor, ingrese un año para consultar los autores que estaban vivos en ese período");
        var anio = teclado.nextInt();
        List<Autor> autoresVivos = autorRepository.buscarAutoresPorAnio(anio);
        System.out.println("---Autores registrados vivos en " + anio + "---");
        autoresVivos.forEach(a -> System.out.println("\n" + a.toString()));
    }

    private void listarLibrosPorIdioma() {
        System.out.println(
                """
                        --------------------
                        Idiomas disponibles:
                        es - Español
                        en - Inglés
                        pt - Portugués
                        fr - Francés
                        it - Italiano
                        de - Alemán
                        ---------------------
                        """
        );
        System.out.println("Por favor, ingrese el idioma que desea buscar");
        var idiomaBuscado = teclado.nextLine();

        try {

            List<Libro> librosPorIdioma = libroRepository.findByIdioma(Idioma.fromString(idiomaBuscado));
            if (!librosPorIdioma.isEmpty()) {
                System.out.println("---Libros registrados publicados en " + Idioma.fromString(idiomaBuscado) + "---");
                librosPorIdioma.forEach(l -> System.out.println("\n" + l.toString()));
            } else {
                System.out.println("No se han encontrado libros en ese idioma.");
            }
        } catch (Exception e) {
            System.out.println(mensajeOpcionInvalida);
        }
    }

    private void topLibrosMasDescargados(){
        System.out.println("---Top 10 de los libros más descargados---");
        List<Libro> top10 = libroRepository.findTop10();
        top10.forEach(l -> System.out.println(l.toString()));
    }

    private void mostrarEstadisticas() {
        System.out.println("---Estadísticas de los libros registrados---");
        libros = libroRepository.findAll();
        DoubleSummaryStatistics estadisticas = libros.stream()
                .filter(l -> l.getNumeroDescargas() > 0.0)
                .collect(Collectors.summarizingDouble(Libro::getNumeroDescargas));
        System.out.println("Número máximo de descargas: " + estadisticas.getMax());
        System.out.println("Número mínimo de descargas: " + estadisticas.getMin());
        System.out.println("Cantidad total de libros registrados: " + estadisticas.getCount());
        System.out.println("Total de descargas de todos los libros registrados: " + estadisticas.getSum());
        System.out.println("Promedio de descargas: " + Math.round(estadisticas.getAverage()));
    }


}

