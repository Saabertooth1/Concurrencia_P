package cc.qp;

import es.upm.babel.cclib.Monitor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class QuePasaMonitor implements QuePasa, Practica {
    /**
     * @attribute miembros Mapa del tipo <K,V> siendo K el nombre del grupo y V una lista con los Uid de los miembros pertenecientes a ese grupo
     * @attribute administrador Mapa del tipo <K,V> siendo K el nombre del grupo y V el Uid del creador del grupo
     * @attribute mensajes Mapa del tipo <K,V> siendo K el Uid del usuario y V una lista LIFO con los mensajes de ese usuario
     * @attrubute condiciones Mapa del tipo <K,V> siendo K el Uid del usuario y V una lista LIFO con las condiciones del monitor
     * @attribute mutex Monitor
     */
    private LinkedList<Integer> usuarios = new LinkedList<Integer>();
    private Map<String, LinkedList<Integer>> miembros = new HashMap<String, LinkedList<Integer>>();
    private Map<String, Integer> administrador = new HashMap<String, Integer>();
    private Map<Integer, LinkedList<Mensaje>> mensajes = new HashMap<Integer, LinkedList<Mensaje>>();
    private Map<Integer, Monitor.Cond> condiciones = new HashMap<Integer, Monitor.Cond>();
    private Monitor mutex = new Monitor();

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
     * Crea un grupo y añade su creador a la lista de miembros del nuevo grupo. Además crea la lista de mensajes para el usuario
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
        administrador.put(grupo, creadorUid);
        LinkedList<Integer> listaMiembros = new LinkedList<Integer>();
        listaMiembros.add(creadorUid);
        miembros.put(grupo, listaMiembros);
        usuarios.add(creadorUid);
        if (!mensajes.containsKey(creadorUid)) {
            LinkedList<Mensaje> mensajes = new LinkedList<Mensaje>();
            this.mensajes.put(creadorUid, mensajes);
        }
        mutex.leave();
    }

    /**
     * Saca un miembro del grupo y borra de la lista de mensajes del usuario asociada a ese grupo
     *
     * @param usuarioUid Uid del miembro a sacar del grupo
     * @param grupo      nombre del grupo del que hay que sacar un miembro
     * @throws PreconditionFailedException si el grupo no existe, el miembro no está dentro del grupo o el miembro es el administrador
     */

    public void salirGrupo(int usuarioUid, String grupo) throws PreconditionFailedException {
        mutex.enter();
        if ((administrador.get(grupo) != null && miembros.get(grupo) != null) && (administrador.get(grupo).equals(usuarioUid) || miembros.get(grupo).contains(usuarioUid))) {
            for (int i = 0; i < this.mensajes.get(usuarioUid).size(); i++) {
                if (this.mensajes.get(usuarioUid).get(i).getGrupo().equals(grupo)) {
                    this.mensajes.get(usuarioUid).remove(i);
                }
            }
            this.miembros.get(grupo).removeFirstOccurrence(usuarioUid);
        } else {
            throw new PreconditionFailedException();
        }
        mutex.leave();
    }

    /**
     * Añade un miembro al grupo y crea una lista de mensajes para el nuevo usuario si esta no existe
     *
     * @param creadorUid      Uid del creador del grupo
     * @param grupo           nombre del grupo al que hay que añadir un miembro
     * @param nuevoMiembroUid Uid del nuevo miembro
     * @throws PreconditionFailedException si creadorUid no es el administrador del grupo o el usuario ya pertenece al grupo
     */

    public void anadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {
        mutex.enter();
        if (administrador.containsValue(creadorUid) && !miembros.get(grupo).contains(nuevoMiembroUid)) {
            miembros.get(grupo).add(nuevoMiembroUid);
            usuarios.add(nuevoMiembroUid);
            if (!mensajes.containsKey(nuevoMiembroUid)) {
                LinkedList<Mensaje> mensajes = new LinkedList<Mensaje>();
                this.mensajes.put(nuevoMiembroUid, mensajes);
            }
        } else {
            throw new PreconditionFailedException();
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
        if (miembros.containsKey(grupo) && miembros.get(grupo).contains(remitenteUid)) {
            Mensaje mensaje = new Mensaje(remitenteUid, grupo, contenidos);
            for (Integer indexMiembro : miembros.get(grupo)) {
                mensajes.get(indexMiembro).addLast(mensaje);
            }
        } else {
            throw new PreconditionFailedException();
        }
        desbloquear();
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
            if (!this.condiciones.containsKey(uid)) {
                Monitor.Cond condicion = mutex.newCond();
                this.condiciones.put(uid, condicion);


            } else {
                this.condiciones.remove(uid);
                Monitor.Cond condicion = mutex.newCond();
                this.condiciones.put(uid,condicion);
            }

            this.condiciones.get(uid).await();

            /**if (this.condiciones.get(uid).isEmpty()) {
                this.condiciones.remove(uid);
            }*/
        }

        Mensaje mensaje = mensajes.get(uid).pop();
        desbloquear();
        mutex.leave();
        return mensaje;
    }

    /**
     * Desbloquea el hilo
     *
     *
     */
    private void desbloquear() {
        boolean aux = false;
        for (int i = 0; i < usuarios.size(); i++) {
            if (!aux && this.usuarios != null && this.usuarios.get(i) != null && this.condiciones.get(usuarios.get(i)) != null
                    && this.condiciones.get(usuarios.get(i)).waiting() > 0 && !this.mensajes.get(usuarios.get(i)).isEmpty()) {
                this.condiciones.get(usuarios.get(i)).signal();
                aux = true;
            }
        }
    }
}