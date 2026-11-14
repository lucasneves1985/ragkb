package br.lcn.ragkb.conversation;



/** Source data persisted and returned as part of a conversation message. */
public record ConversationSourceReference(String type, String label, String url) {
}
