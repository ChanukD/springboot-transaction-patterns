package com.demo.tx.readonly;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.demo.tx.common.DemoRunner.ACCOUNT_ID;

@RestController
@RequestMapping("/demo/readonly")
public class ReadOnlyController {

    private final ProfileService profiles;

    public ReadOnlyController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @PostMapping("/modify-in-readonly")
    public Map<String, String> modifyInReadOnly() {
        String returned = profiles.displayNameInReadOnly(ACCOUNT_ID);
        return Map.of(
                "valueInsideMethod", returned,
                "valueInDatabase", profiles.owner(ACCOUNT_ID),
                "lesson", "readOnly=true: Hibernate skips dirty checking and flush, so the change was never saved.");
    }

    @PostMapping("/rename")
    public Map<String, String> rename(@RequestParam String owner) {
        profiles.rename(ACCOUNT_ID, owner);
        return Map.of(
                "valueInDatabase", profiles.owner(ACCOUNT_ID),
                "lesson", "Read-write tx: dirty checking saved the change without calling save().");
    }
}
