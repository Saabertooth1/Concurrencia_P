// QuePasaCSP.java
// Lars-Ake Fredlund y Julio Mariño
// Esqueleto para la realización de la práctica por paso de mensajes
// Completad las líneas marcadas con "TO DO"
// Solución basada en peticiones aplazadas
// Los huecos son orientativos: se basan en nuestra propia
// implementación (incluyendo comentarios).
package cc.qp;

import org.jcsp.lang.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


public class QuePasaCSP implements QuePasa, CSProcess, Practica {
    /**
     * Creamos un canal para cada operación sin CPRE
     * Creamos también un canal para solicitar la lectura
     * Usaremos peticiones aplazadas en el servidor para tratar la CPRE de lectura
     */
    private Any2OneChannel chCrearGrupo = Channel.any2one();
    private Any2OneChannel chAnadirMiembro = Channel.any2one();
    private Any2OneChannel chSalirGrupo = Channel.any2one();
    private Any2OneChannel chMandarMensaje = Channel.any2one();
    private Any2OneChannel chPetLeer = Channel.any2one();

    public QuePasaCSP() {
    }

    /**
     * Obtiene los nombres de los alumnos que forman parte del grupo
     *
     * @return Alumnos que forman parte del grupo
     */
    public Alumno[] getAutores() {
        return new Alumno[]{
                new Alumno("Ignacio de las Alas-Pumariño Martínez", "160066"),
                new Alumno("Iñigo Aranguren Redondo", "160054")
        };
    }

    /**
     * Crea la petición para crear un nuevo grupo
     *
     * @param creadorUid Uid del creador del grupo
     * @param grupo      nombre del grupo
     * @throws PreconditionFailedException si el estado de la petición es negativo
     */
    public void crearGrupo(int creadorUid, String grupo)
            throws PreconditionFailedException {
        // creamos la petición
        PetCrearGrupo pet = new PetCrearGrupo(creadorUid, grupo);
        // la enviamos
        chCrearGrupo.out().write(pet);
        // recibimos mensaje de status
        Boolean exito = (Boolean) pet.chResp.in().read();
        // si el estado de la petición es negativo, lanzamos excepción
        if (!exito)
            throw new PreconditionFailedException();
    }

    /**
     * Crea la petición para añadir un nuevo miembro a un grupo
     *
     * @param uid             Uid del administrador del grupo
     * @param grupo           nombre del grupo al que se va a añadir al usuario
     * @param nuevoMiembroUid Uid del nuevo miembro
     * @throws PreconditionFailedException si el estado de la petición es negativo
     */
    public void anadirMiembro(int uid, String grupo, int nuevoMiembroUid)
            throws PreconditionFailedException {

        // creamos la petición
        PetAnadirMiembro pet = new PetAnadirMiembro(uid, grupo, nuevoMiembroUid);

        //la enviamos
        chAnadirMiembro.out().write(pet);
        //recibimos mensaje de status

        Boolean exito = (Boolean) pet.chResp.in().read();
        // si el estado de la petición es negativo, lanzamos excepción
        if (!exito)
            throw new PreconditionFailedException();

    }

    /**
     * Crea la petición para que un usuario salga de un grupo
     *
     * @param uid   Uid del usuario que va a salir del grupo
     * @param grupo nombre del grupo del que se quiere salir el usuario
     * @throws PreconditionFailedException si el estado de la petición es negativo
     */
    public void salirGrupo(int uid, String grupo)
            throws PreconditionFailedException {
        // creamos la petición
        PetSalirGrupo pet = new PetSalirGrupo(uid, grupo);

        //la enviamos
        chSalirGrupo.out().write(pet);
        //recibimos mensaje de status

        Boolean exito = (Boolean) pet.chResp.in().read();
        // si el estado de la petición es negativo, lanzamos excepción
        if (!exito)
            throw new PreconditionFailedException();
    }

    /**
     * Crea la petición para que un usuario mande un mensaje a un grupo
     *
     * @param remitenteUid Uid del remitente
     * @param grupo        nombre del grupo al que se envía el mensaje
     * @param contenidos   contenido del mensaje
     * @throws PreconditionFailedException si el estado de la petición es negativo
     */
    public void mandarMensaje(int remitenteUid, String grupo, Object contenidos)
            throws PreconditionFailedException {
        // creamos la petición
        PetMandarMensaje pet = new PetMandarMensaje(remitenteUid, grupo, contenidos);

        //la enviamos
        chMandarMensaje.out().write(pet);
        //recibimos mensaje de status

        Boolean exito = (Boolean) pet.chResp.in().read();
        // si el estado de la petición es negativo, lanzamos excepción
        if (!exito)
            throw new PreconditionFailedException();
    }

    /**
     * Crea la petición para que un usuario lea sus mensajes
     *
     * @param uid Uid del usuario que lee los mensajes
     * @return mensaje leído
     */
    public Mensaje leer(int uid) {

        // creamos la petición
        PetLeer pet = new PetLeer(uid);

        //la enviamos
        chPetLeer.out().write(pet);
        //recibimos mensaje de status

        return (Mensaje) pet.chResp.in().read();

    }

    // Implementamos aquí los métodos de la interfaz QuePasa
    // os regalamos la implementación de crearGrupo

    /**
     * Método run donde se encuentra el servidor
     */
    public void run() {

        //Asignación de los canales
        chCrearGrupo = Channel.any2one();
        chAnadirMiembro = Channel.any2one();
        chSalirGrupo = Channel.any2one();
        chMandarMensaje = Channel.any2one();
        chPetLeer = Channel.any2one();

        //Creación de los HashMaps para guardar la información
        HashMap<String, LinkedList<Integer>> miembros = new HashMap<String, LinkedList<Integer>>();
        HashMap<String, Integer> administrador = new HashMap<String, Integer>();
        HashMap<Integer, LinkedList<Mensaje>> mensajes = new HashMap<Integer, LinkedList<Mensaje>>();
        HashMap<Integer, LinkedList<PetLeer>> condiciones = new HashMap<Integer, LinkedList<PetLeer>>();

        // Códigos de peticiones para facilitar la asociación
        // de canales a operaciones
        final int CREAR_GRUPO = 0;
        final int ANADIR_MIEMBRO = 1;
        final int SALIR_GRUPO = 2;
        final int MANDAR_MENSAJE = 3;
        final int LEER = 4;

        // recepción alternativa
        final Guard[] guards = new AltingChannelInput[5];
        guards[CREAR_GRUPO] = chCrearGrupo.in();
        guards[ANADIR_MIEMBRO] = chAnadirMiembro.in();
        guards[SALIR_GRUPO] = chSalirGrupo.in();
        guards[MANDAR_MENSAJE] = chMandarMensaje.in();
        guards[LEER] = chPetLeer.in();

        final Alternative services = new Alternative(guards);
        int chosenService;

        while (true) {
            // toda recepcion es incondicional
            chosenService = services.fairSelect();
            switch (chosenService) {
                // regalamos la implementación del servicio de crearGrupo
                case CREAR_GRUPO: {
                    // recepción del mensaje
                    PetCrearGrupo pet = (PetCrearGrupo) chCrearGrupo.in().read();
                    // comprobación de la PRE
                    if (administrador.containsKey(pet.grupo)) {

                        pet.chResp.out().write(false);

                    }
                    // ejecución normal
                    else {

                        administrador.put(pet.grupo, pet.creadorUid);

                        LinkedList<Integer> listaMiembros = new LinkedList<Integer>();
                        listaMiembros.add(pet.creadorUid);
                        miembros.put(pet.grupo, listaMiembros);

                        if (mensajes.get(pet.creadorUid) == null) {
                            LinkedList<Mensaje> listaMensajes = new LinkedList<Mensaje>();
                            mensajes.put(pet.creadorUid, listaMensajes);
                        }

                        pet.chResp.out().write(true);
                    }
                    break;
                }
                case ANADIR_MIEMBRO: {
                    // recepcion del mensaje
                    PetAnadirMiembro pet = (PetAnadirMiembro) chAnadirMiembro.in().read();
                    // comprobacion de la PRE
                    if (!administrador.containsKey(pet.grupo) || administrador.get(pet.grupo) != pet.creadorUid || administrador.get(pet.grupo) == pet.nuevoMiembroUid || (miembros.get(pet.grupo) != null && miembros.get(pet.grupo).contains(pet.nuevoMiembroUid))) {

                        pet.chResp.out().write(false);

                    }
                    // ejecucion normal
                    else {


                        miembros.get(pet.grupo).add(pet.nuevoMiembroUid);

                        if (mensajes.get(pet.nuevoMiembroUid) == null) {
                            LinkedList<Mensaje> listaMensajes = new LinkedList<Mensaje>();
                            mensajes.put(pet.nuevoMiembroUid, listaMensajes);
                        }

                        pet.chResp.out().write(true);

                    }
                    break;
                }
                case SALIR_GRUPO: {
                    // recepcion de la peticion
                    PetSalirGrupo pet = (PetSalirGrupo) chSalirGrupo.in().read();
                    // comprobacion de la PRE
                    if (miembros.get(pet.grupo) == null || (administrador.containsKey(pet.grupo) && administrador.get(pet.grupo) == pet.usuarioUid) || !miembros.get(pet.grupo).contains(pet.usuarioUid)) {

                        pet.chResp.out().write(false);

                    }
                    // ejecucion normal
                    else {

                        miembros.get(pet.grupo).removeFirstOccurrence(pet.usuarioUid);

                        if (mensajes.get(pet.usuarioUid) != null) {


                            LinkedList<Mensaje> listaMensajes = mensajes.get(pet.usuarioUid);
                            Iterator<Mensaje> pos = listaMensajes.iterator();

                            while (pos.hasNext()) {
                                if (pos.next().getGrupo().equals(pet.grupo)) {
                                    pos.remove();
                                }
                            }
                        }
                        pet.chResp.out().write(true);
                    }
                    break;
                }
                case MANDAR_MENSAJE: {
                    // recepcion de la peticion
                    PetMandarMensaje pet = (PetMandarMensaje) chMandarMensaje.in().read();
                    // comprobacion de la PRE
                    if (!miembros.containsKey(pet.grupo) || !miembros.get(pet.grupo).contains(pet.remitenteUid)) {

                        pet.chResp.out().write(false);

                    }
                    // ejecucion normal
                    else {


                        LinkedList<Integer> miembro = miembros.get(pet.grupo);
                        Mensaje resultado = new Mensaje(pet.remitenteUid, pet.grupo, pet.contenidos);

                        for (Integer aMiembro : miembro) {

                            LinkedList<Mensaje> listaMensajes = mensajes.get(aMiembro);
                            listaMensajes.add(resultado);
                            mensajes.put(aMiembro, listaMensajes);
                        }

                        pet.chResp.out().write(true);
                    }
                    break;
                }
                case LEER: {
                    // recepcion de la peticion
                    PetLeer pet = (PetLeer) chPetLeer.in().read();
                    // no hay PRE que comprobar!
                    // TO DO: aquí lo más sencillo
                    // TO DO  es guardar la petición
                    // TO DO  según se recibe
                    // TO DO  (reutilizad la estructura que
                    // TO DO   usasteis en monitores
                    // TO DO   cambiando Cond por One2OneChannel)

                    if (condiciones.get(pet.uid) == null) {

                        LinkedList<PetLeer> lecturas = new LinkedList<PetLeer>();
                        lecturas.add(pet);
                        condiciones.put(pet.uid, lecturas);

                    } else {
                        condiciones.get(pet.uid).add(pet);
                    }
                    break;
                }
            } // END SWITCH

            PetLeer pet;
            Mensaje msg;

            for (Integer i : condiciones.keySet()) {

                if (mensajes.get(i) != null && !mensajes.get(i).isEmpty()) {

                    if (condiciones.get(i) == null) {

                        LinkedList<PetLeer> lecturas = new LinkedList<PetLeer>();
                        condiciones.put(i, lecturas);

                    } else if (!condiciones.get(i).isEmpty()) {

                        while (!mensajes.get(i).isEmpty() && !condiciones.get(i).isEmpty()) {

                            msg = mensajes.get(i).removeFirst();
                            pet = condiciones.get(i).removeFirst();
                            pet.chResp.out().write(msg);
                        }
                    }
                }
            }
        } // END while(true) SERVIDOR
    } // END run()

    /**
     * Clases Auxiliares con sus constructores
     */
    public class PetCrearGrupo {
        public int creadorUid;
        public String grupo;
        // para tratamiento de la PRE
        public One2OneChannel chResp;

        public PetCrearGrupo(int creadorUid, String grupo) {
            this.creadorUid = creadorUid;
            this.grupo = grupo;
            this.chResp = Channel.one2one();
        }
    }

    public class PetAnadirMiembro {

        public int creadorUid;
        public String grupo;
        public int nuevoMiembroUid;

        // para tratamiento de la PRE
        public One2OneChannel chResp;

        public PetAnadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) {
            this.creadorUid = creadorUid;
            this.grupo = grupo;
            this.nuevoMiembroUid = nuevoMiembroUid;
            this.chResp = Channel.one2one();
        }
    }

    public class PetSalirGrupo {

        public int usuarioUid;
        public String grupo;

        // para tratamiento de la PRE
        public One2OneChannel chResp;

        public PetSalirGrupo(int usuarioUid, String grupo) {

            this.usuarioUid = usuarioUid;
            this.grupo = grupo;
            this.chResp = Channel.one2one();
        }
    }

    public class PetMandarMensaje {

        public int remitenteUid;
        public String grupo;
        public Object contenidos;

        // para tratamiento de la PRE
        public One2OneChannel chResp;

        public PetMandarMensaje(int remitenteUid, String grupo, Object contenidos) {

            this.remitenteUid = remitenteUid;
            this.grupo = grupo;
            this.contenidos = contenidos;
            this.chResp = Channel.one2one();

        }
    }

    public class PetLeer {

        public int uid;

        // para tratamiento de la PRE
        public One2OneChannel chResp;

        public PetLeer(int uid) {

            this.uid = uid;
            this.chResp = Channel.one2one();

        }
    }
} // END class QuePasaCSP
