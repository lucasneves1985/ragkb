package br.lcn.ragkb.repository;

/**
 * Projeção da busca semântica híbrida: id do chunk + score composto. lexical =
 * true quando o conteúdo casa por ILIKE (score tratado como 1.0 no service);
 * similarity = similaridade de cosseno (1 - distancia) quando o match é
 * vetorial.
 */
public interface CardChunkScore {

    String getId();

    Boolean getLexical();

    Double getSimilarity();
}
