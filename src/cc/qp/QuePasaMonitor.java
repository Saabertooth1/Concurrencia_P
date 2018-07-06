package cc.qp;

import es.upm.babel.cclib.Monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;


public class QuePasaMonitor implements QuePasa, Practica {
    /**
     * @attribute miembros Mapa del tipo <K,V> siendo K el nombre del grupo y V una lista con los Uid de los miembros pertenecientes a ese grupo
     * @attribute administrador Mapa del tipo <K,V> siendo K el nombre del grupo y V el Uid del creador del grupo
     * @attribute mensajes Mapa del tipo <K,V> siendo K el Uid del usuario y V una lista LIFO con los mensajes de ese usuario
     * @attribute mutex Monitor
     * @attribute condiciones Mapa del tipo <K,V> siendo K el Uid del usuario y V la condicion de lectura
     * @attribute cLeer condición para que el metodo leer pueda bloquear y desbloquear
     * @attribute usuarios ArrayList de todos los usuarios del programa. No hay usuarios duplicados
     */
    private HashMap<String, LinkedList<Integer>> miembros = new HashMap<String, LinkedList<Integer>>();
    private HashMap<String, Integer> administrador = new HashMap<String, Integer>();
    private HashMap<Integer, LinkedList<Mensaje>> mensajes = new HashMap<Integer, LinkedList<Mensaje>>();
    private HashMap<Integer, Monitor.Cond> condiciones = new HashMap<Integer, Monitor.Cond>();
    private Monitor mutex;
    private Monitor.Cond cLeer;
    private ArrayList<Integer> usuarios = new ArrayList<Integer>();

    /**
     * Constructor
     */

    QuePasaMonitor() {
        mutex = new Monitor();
        cLeer = mutex.newCond();
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
     * Crea un grupo y añade su creador a la lista de miembros del nuevo grupo y a la lista usuarios. Además crea la lista de mensajes para el usuario
     * creador del grupo si esta no existe
     *
     * @param creadorUid Uid del creador del grupo
     * @param grupo      nombre del grupo
     * @throws PreconditionFailedException si el grupo ya existe
     */

    public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
        mutex.enter();

        if (administrador.containsKey(grupo)) {

            mutex.leave();
            throw new PreconditionFailedException();
        }

        usuarios.add(creadorUid);

        administrador.put(grupo, creadorUid);

        LinkedList<Integer> listaMiembros = new LinkedList<Integer>();
        listaMiembros.add(creadorUid);
        miembros.put(grupo, listaMiembros);

        if (mensajes.get(creadorUid) == null) {
            LinkedList<Mensaje> listaMensajes = new LinkedList<Mensaje>();
            mensajes.put(creadorUid, listaMensajes);
        }


        mutex.leave();
    }

    /**
     * Saca un miembro del grupo y de la lista de usuarios ademas de borrar los mensajes de ese usuario en ese grupo
     *
     * @param usuarioUid Uid del miembro a sacar del grupo
     * @param grupo      nombre del grupo del que hay que sacar un miembro
     * @throws PreconditionFailedException si el grupo no existe, el miembro no está dentro del grupo o el miembro es el administrador
     */

    public void salirGrupo(int usuarioUid, String grupo) throws PreconditionFailedException {

        mutex.enter();

        if (miembros.get(grupo) == null || (administrador.containsKey(grupo) && administrador.get(grupo) == usuarioUid) || !miembros.get(grupo).contains(usuarioUid)) {

            mutex.leave();
            throw new PreconditionFailedException();
        }

        for (int usuario = 0; usuario < this.usuarios.size(); usuario++) {

            if (usuario == usuarioUid) {

                this.usuarios.remove(usuarioUid);

            }
        }

        miembros.get(grupo).removeFirstOccurrence(usuarioUid);

        if (mensajes.get(usuarioUid) != null) {


            LinkedList<Mensaje> listaMensajes = mensajes.get(usuarioUid);
            Iterator<Mensaje> pos = listaMensajes.iterator();

            while (pos.hasNext()) {
                if (pos.next().getGrupo().equals(grupo)) {
                    pos.remove();
                }
                ;
            }
        }

        mutex.leave();
    }

    /**
     * Añade un miembro al grupo y a la lista de usuarios y crea una lista de mensajes para el nuevo usuario si esta no existe
     *
     * @param creadorUid      Uid del creador del grupo
     * @param grupo           nombre del grupo al que hay que añadir un miembro
     * @param nuevoMiembroUid Uid del nuevo miembro
     * @throws PreconditionFailedException si creadorUid no es el administrador del grupo o el usuario ya pertenece al grupo
     */

    public void anadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {

        mutex.enter();

        if (!administrador.containsKey(grupo) || administrador.get(grupo) != creadorUid || administrador.get(grupo) == nuevoMiembroUid || (miembros.get(grupo) != null && miembros.get(grupo).contains(nuevoMiembroUid))) {

            mutex.leave();
            throw new PreconditionFailedException();

        } else {

            miembros.get(grupo).add(nuevoMiembroUid);

            boolean encontrado = false;

            for (int i = 0; i < usuarios.size(); i++) {

                if (usuarios.get(i).equals(nuevoMiembroUid)) {

                    encontrado = true;
                }
            }

            if (!encontrado) {

                this.usuarios.add(nuevoMiembroUid);
            }

            if (mensajes.get(nuevoMiembroUid) == null) {
                LinkedList<Mensaje> listaMensajes = new LinkedList<Mensaje>();
                mensajes.put(nuevoMiembroUid, listaMensajes);
            }
        }
        mutex.leave();
    }

    /**
     * Añade el mensaje a la lista de mensajes de todos los miembros de un grupo
     *
     * @param remitenteUid Uid del remitente del mensaje
     * @param grupo        nombre del grupo en el que se manda el mensaje
     * @param contenidos   contenido del mensaje
     * @throws PreconditionFailedException si el grupo no existe o el remitente no es miembro del grupo
     */

    public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {

        mutex.enter();

        if (!miembros.containsKey(grupo) || !miembros.get(grupo).contains(remitenteUid)) {

            mutex.leave();
            throw new PreconditionFailedException();
        }

        LinkedList<Integer> miembro = miembros.get(grupo);
        Mensaje resultado = new Mensaje(remitenteUid, grupo, contenidos);

        for (int i = 0; i < miembro.size(); i++) {

            LinkedList<Mensaje> listaMensajes = mensajes.get(miembro.get(i));
            listaMensajes.addLast(resultado);
            mensajes.put(miembro.get(i), listaMensajes);
        }

        desbloqueo();

        mutex.leave();

    }

    /**
     * Comprueba que los procesos sean accesibles por el mutex y devuelve los mensajes siguiendo un orden LIFO
     *
     * @param uid uid del usuario que lee sus mensajes
     * @return mensaje leído
     */

    public Mensaje leer(int uid) {

        mutex.enter();

        if (!mensajes.containsKey(uid) || mensajes.get(uid).isEmpty()) {

            if (this.condiciones.get(uid) == null || this.condiciones.isEmpty()) {

                Monitor.Cond lectura = mutex.newCond();
                this.condiciones.put(uid, lectura);
            }

            this.condiciones.get(uid).await();


        }

        LinkedList<Mensaje> listaNueva = mensajes.get(uid);
        Mensaje resultado = listaNueva.pop();

        mensajes.remove(uid);
        mensajes.put(uid, listaNueva);

        cLeer.signal();

        mutex.leave();

        return resultado;
    }

    /**
     * Comprueba que existan usuarios en la lista usuarios. Comprueba si alguno de los usuarios de la lista tienen un waiting > 0 le hace un signal para desbloquearlo.
     */

    private void desbloqueo() {

        for (int usuario = 0; usuario < usuarios.size(); usuario++) {

            if (this.usuarios != null && this.usuarios.get(usuario) != null && this.condiciones.get(usuarios.get(usuario)) != null && this.condiciones.get(usuarios.get(usuario)).waiting() > 0
                    && !this.mensajes.get(usuarios.get(usuario)).isEmpty()) {

                this.condiciones.get(usuarios.get(usuario)).signal();
                cLeer.await();
            }
        }
    }
}
