package ir.mohaymen.iris.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import ir.mohaymen.iris.user.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthDto {
  @JsonProperty("access_token")
  protected String accessToken;
  @JsonProperty("refresh_token")
  protected String refreshToken;
  private UserDto user;
  private boolean isRegistered;
}
