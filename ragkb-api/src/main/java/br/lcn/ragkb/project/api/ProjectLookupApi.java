package br.lcn.ragkb.project.api;







import java.util.Optional;

import br.lcn.ragkb.project.api.ProjectLookupApi.ProjectAccess;

/** Narrow project access API used by cards without exposing ProjectRepository. */
public interface ProjectLookupApi {
    Optional<ProjectAccess> findVisibleProject(String projectId, Long userId);

    Optional<ProjectAccess> findProject(String projectId);

    boolean isParticipant(String projectId, Long userId);

    record ProjectAccess(String id, Long ownerId, String name) {
        public boolean isLegacy() {
            return ownerId == null;
        }
    }
}
