package br.lcn.ragkb.identity.api;



/** Narrow read API for resolving a user's sector without exposing identity persistence. */
public interface SectorLookupApi {
    String findSectorByUsername(String username);
}
