package demo.quarkus.reactive.user.profile;

public record UserProfileFetch(String username, String email, String deviceId, String city, Boolean makePublic) {
}
