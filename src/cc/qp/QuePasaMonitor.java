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
     * @attribute mutex Monitor
     */
    private Map<String, LinkedList<Integer>> miembros = new HashMap<String, LinkedList<Integer>>();
    private Map<String, Integer> administrador = new HashMap<String, Integer>();
    private Map<Integer, LinkedList<Mensaje>> mensajes = new HashMap<Integer, LinkedList<Mensaje>>();
    private LinkedList<Monitor.Cond> conditions = new LinkedList<Monitor.Cond>();
    private LinkedList<Integer> usuarios = new LinkedList<Integer>();
    //private Map<Integer, Integer> condiciones = new HashMap<Integer, Integer>();
    private Monitor mutex;
    //private Monitor.Cond cLeer;

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

    QuePasaMonitor(){
        mutex = new Monitor();
        //cLeer =  mutex.newCond();
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
        if (!usuarios.contains(creadorUid)){
            Monitor.Cond cLeer = mutex.newCond();
            conditions.addLast(cLeer);
            usuarios.addLast(creadorUid);
        }
        if (!mensajes.containsKey(creadorUid)) {
            LinkedList<Mensaje> mensajesAux = new LinkedList<Mensaje>();
            mensajes.put(creadorUid, mensajesAux);
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
            LinkedList<Mensaje> aux = mensajes.get(usuarioUid);
            for (int i = 0; i < aux.size(); i++) {
                if (aux.get(i).getGrupo().equals(grupo)) {
                    aux.remove(i);
                }
            }
            mensajes.put(usuarioUid, aux);
            LinkedList<Integer> aux2 = miembros.get(grupo);
            aux2.removeFirstOccurrence(usuarioUid);
            miembros.put(grupo, aux2);
        } else {
            mutex.leave();
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
            LinkedList<Integer> aux = miembros.get(grupo);
            aux.add(nuevoMiembroUid);
            miembros.put(grupo, aux);
            if (!usuarios.contains(nuevoMiembroUid)){
                Monitor.Cond cLeer = mutex.newCond();
                conditions.addLast(cLeer);
                usuarios.addLast(nuevoMiembroUid);
            }
            if (!mensajes.containsKey(nuevoMiembroUid)) {
                LinkedList<Mensaje> mensajes = new LinkedList<Mensaje>();
                this.mensajes.put(nuevoMiembroUid, mensajes);
            }
        } else {
            mutex.leave();
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
        if (!miembros.containsKey(grupo) || !miembros.get(grupo).contains(remitenteUid)) {
            mutex.leave();
            throw new PreconditionFailedException();
        }

        Mensaje mensaje = new Mensaje(remitenteUid, grupo, contenidos);
        LinkedList<Integer> miembros = this.miembros.get(grupo);
        for (int i = 0; i < miembros.size(); i++){
            LinkedList<Mensaje> aux = mensajes.get(miembros.get(i));
            aux.addLast(mensaje);
            mensajes.put(miembros.get(i), aux);
        }
        unblock();
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
        Monitor.Cond cLeer;
        if (!mensajes.containsKey(uid) || mensajes.get(uid).isEmpty()) {
            if (!usuarios.contains(uid)){
                cLeer = mutex.newCond();
                conditions.addLast(cLeer);
                usuarios.addLast(uid);
            }
            conditions.get(usuarios.indexOf(uid)).await();
        }
        LinkedList<Mensaje> aux = mensajes.get(uid);
        Mensaje mensaje = aux.pop();
        mensajes.put(uid, aux);
        unblock();
        mutex.leave();
        return mensaje;
    }

    private void unblock(){
        if (!conditions.isEmpty()){
            for (int i = 0; i < conditions.size(); i++){
                if (mensajes.containsKey(usuarios.get(i)) && !mensajes.get(usuarios.get(i)).isEmpty()){
                    conditions.get(i).signal();
                    break;
                }
            }
        }
    }
}