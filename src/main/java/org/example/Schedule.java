package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@Setter
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;
    private String teacherName;
    private String auditorium;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;
}


interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByGroupName(String groupName);
    List<Schedule> findByTeacherName(String teacherName);
    List<Schedule> findByAuditorium(String auditorium);
}


@Service
class ScheduleService {
    @Autowired
    private ScheduleRepository repository;

    public List<Schedule> getAllSchedules() {
        return repository.findAll();
    }

    public Optional<Schedule> getScheduleById(Long id) {
        return repository.findById(id);
    }

    public Schedule createSchedule(Schedule schedule) {
        if (hasConflict(schedule)) {
            throw new RuntimeException("Schedule conflict detected!");
        }
        return repository.save(schedule);
    }

    public Schedule updateSchedule(Long id, Schedule updatedSchedule) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Schedule not found");
        }
        updatedSchedule.setId(id);
        if (hasConflict(updatedSchedule)) {
            throw new RuntimeException("Schedule conflict detected!");
        }
        return repository.save(updatedSchedule);
    }

    public void deleteSchedule(Long id) {
        repository.deleteById(id);
    }

    private boolean hasConflict(Schedule newSchedule) {
        List<Schedule> schedules = repository.findAll();
        for (Schedule existing : schedules) {
            boolean timeOverlap = newSchedule.getStartTime().isBefore(existing.getEndTime()) &&
                    newSchedule.getEndTime().isAfter(existing.getStartTime());
            boolean sameResource = newSchedule.getTeacherName().equals(existing.getTeacherName()) ||
                    newSchedule.getAuditorium().equals(existing.getAuditorium()) ||
                    newSchedule.getGroupName().equals(existing.getGroupName());

            if (timeOverlap && sameResource) {
                return true;
            }
        }
        return false;
    }
}


@RestController
@RequestMapping("/schedules")
class ScheduleController {
    @Autowired
    private ScheduleService service;

    @GetMapping
    public List<Schedule> getAllSchedules() {
        return service.getAllSchedules();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable Long id) {
        return service.getScheduleById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Schedule> createSchedule(@RequestBody Schedule schedule) {
        try {
            return new ResponseEntity<>(service.createSchedule(schedule), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Schedule> updateSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        try {
            return new ResponseEntity<>(service.updateSchedule(id, schedule), HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.CONFLICT);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        service.deleteSchedule(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
