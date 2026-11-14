package br.lcn.ragkb.reminder;



import jakarta.validation.constraints.NotBlank;

public record ReminderCreateRequest(
        @NotBlank(message = "request é obrigatório (texto livre do lembrete)")
        String request,
        String conversationId
        ) {

}
