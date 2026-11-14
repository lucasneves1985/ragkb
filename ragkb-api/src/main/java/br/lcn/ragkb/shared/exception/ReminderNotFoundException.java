package br.lcn.ragkb.shared.exception;



public class ReminderNotFoundException extends RuntimeException {

    public ReminderNotFoundException(String id) {
        super("Lembrete não encontrado: " + id);
    }
}
