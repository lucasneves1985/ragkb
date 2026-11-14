package br.lcn.ragkb.identity.api;



import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Narrow user lookup API for modules that need card ownership/display data. */
public interface UserLookupApi extends SectorLookupApi {
    Optional<Long> findIdByUsername(String username);

    Optional<String> findUsernameById(Long userId);

    Map<Long, String> findUsernamesByIds(Collection<Long> userIds);

    Optional<UserContact> findContactById(Long userId);

    Optional<UserContact> findContactByUsername(String username);

    record UserContact(Long id, String username, String phone, String fullName) {
    }
}
