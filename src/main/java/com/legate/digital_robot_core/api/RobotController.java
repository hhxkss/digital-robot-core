//package com.legate.digital_robot_core.api;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.legate.digital_robot_core.serial.RobotSerialService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/v1/robot")
//public class RobotController {
//    private final RobotSerialService serial;
//    private final ObjectMapper om = new ObjectMapper();
//
//    public RobotController(RobotSerialService serial) { this.serial = serial; }
//
//    /** 统一命令风格：{method,id,data} */
//    @PostMapping("/command")
//    public ResponseEntity<?> command(@RequestBody Map<String, Object> req) {
//        String method = (String) req.getOrDefault("method", "");
//        Map<String,Object> data = om.convertValue(req.getOrDefault("data", Map.of()), Map.class);
//
//        switch (method) {
//            case "robot.moveTo" -> {
//                double x = ((Number) data.getOrDefault("x", 0)).doubleValue();
//                double y = ((Number) data.getOrDefault("y", 0)).doubleValue();
//                double yaw = ((Number) data.getOrDefault("yaw", 0)).doubleValue();
//                serial.navGoal(x,y,yaw);
//            }
//            case "robot.navPoint" -> serial.navPoint((String) data.getOrDefault("name", "A"));
//            case "robot.stop"     -> serial.navCancel();
//            case "robot.pause"    -> serial.navPause();
//            case "robot.resume"   -> serial.navResume();
//            case "robot.dock"     -> serial.dockStart();
//            case "robot.pose"     -> serial.getPose();
//            case "robot.maxVel"   -> serial.setMaxVel(((Number)data.getOrDefault("v",0.6)).doubleValue());
//            default -> { return ResponseEntity.badRequest().body(Map.of("code",400,"message","unknown method")); }
//        }
//        return ResponseEntity.ok(Map.of("code",0,"message","ok"));
//    }
//}
