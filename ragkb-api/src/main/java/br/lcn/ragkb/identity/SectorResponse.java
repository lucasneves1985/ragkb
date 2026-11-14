package br.lcn.ragkb.identity;



public record SectorResponse(Long id, String name) {
    public static SectorResponse from(Sector sector) {
        return new SectorResponse(sector.getId(), sector.getName());
    }
}