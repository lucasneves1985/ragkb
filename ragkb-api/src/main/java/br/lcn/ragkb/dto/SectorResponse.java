package br.lcn.ragkb.dto;

import br.lcn.ragkb.entity.Sector;

public record SectorResponse(Long id, String name) {
    public static SectorResponse from(Sector sector) {
        return new SectorResponse(sector.getId(), sector.getName());
    }
}