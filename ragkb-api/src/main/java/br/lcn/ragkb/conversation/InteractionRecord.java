package br.lcn.ragkb.conversation;



import java.util.List;

/** Neutral conversation boundary record used by assistant flows. */
public record InteractionRecord(
        String sender,
        String content,
        String status,
        List<Source> sources,
        String ticketQuestion,
        String ticketUserId) {

    public record Source(String type, String label, String url) {
    }
}
