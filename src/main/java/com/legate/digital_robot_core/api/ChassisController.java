package com.legate.digital_robot_core.api;

import com.legate.digital_robot_core.service.RobotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${robot.chassis.api.base-path:/api/v1/chassis}")
@ConditionalOnProperty(prefix = "robot.chassis.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Validated
public class ChassisController {

    private final RobotService robotService;

    @PostMapping("/nav/point")
    public ResponseEntity<CommandResponse> navToPoint(@RequestBody(required = false) NavPointRequest body) {
        robotService.navToPoint(body != null ? body.point() : null);
        return ok();
    }

    @PostMapping("/nav/cancel")
    public ResponseEntity<CommandResponse> navCancel() {
        robotService.navCancel();
        return ok();
    }

    @PostMapping("/nav/pause")
    public ResponseEntity<CommandResponse> navPause() {
        robotService.navPause();
        return ok();
    }

    @PostMapping("/nav/resume")
    public ResponseEntity<CommandResponse> navResume() {
        robotService.navResume();
        return ok();
    }

    @PostMapping("/dock/start")
    public ResponseEntity<CommandResponse> dockStart() {
        robotService.dockStart();
        return ok();
    }

    @PostMapping("/dock/stop")
    public ResponseEntity<CommandResponse> dockStop() {
        robotService.dockCancel();
        return ok();
    }

    @PostMapping("/nav/max-vel")
    public ResponseEntity<CommandResponse> setMaxVelocity(@Valid @RequestBody MaxVelocityRequest request) {
        robotService.setMaxVel(request.value());
        return ok();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommandResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Chassis command rejected: {}", e.getMessage());
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommandResponse> handleValidationErrors(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage() != null ? err.getDefaultMessage() : err.toString())
                .orElse("Invalid request");
        return badRequest(message);
    }

    private ResponseEntity<CommandResponse> ok() {
        return ResponseEntity.ok(new CommandResponse(0, "ok"));
    }

    private ResponseEntity<CommandResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new CommandResponse(400, message));
    }

    public record NavPointRequest(String point) { }

    public record MaxVelocityRequest(@NotNull(message = "value is required") Double value) { }

    public record CommandResponse(int code, String message) { }
}
