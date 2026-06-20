package io.github.edmaputra.enhauthserv.adapter.in.http;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggedOutController {

  @GetMapping("/logged-out")
  public String loggedOut() {
    return "You have been signed out.";
  }
}
