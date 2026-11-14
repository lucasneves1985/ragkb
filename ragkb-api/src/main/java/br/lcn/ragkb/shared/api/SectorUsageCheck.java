package br.lcn.ragkb.shared.api;


import java.util.Optional;

/** Domain supplied guard used before an identity sector is deleted. */
public interface SectorUsageCheck {
    Optional<String> conflictMessage(String sectorName);
}
