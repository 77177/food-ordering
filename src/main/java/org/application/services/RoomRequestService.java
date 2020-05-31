package org.application.services;

import org.application.models.Room;
import org.application.models.custom.RequestRecord;
import org.application.models.requests.RoomRequest;
import org.application.models.users.AppUser;
import org.application.models.users.Trainer;
import org.application.repositories.RoomRepo;
import org.application.repositories.custom.RequestRecordRepo;
import org.application.repositories.requests.RoomRequestRepo;
import org.application.repositories.users.AppUserRepo;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class RoomRequestService {

    private RequestRecordRepo requestRecordRepo;

    private RoomRequestRepo roomRequestRepo;

    private AppUserRepo appUserRepo;

    private RoomRepo roomRepo;

    public RoomRequestService(RoomRequestRepo roomRequestRepo, AppUserRepo appUserRepo, RoomRepo roomRepo, RequestRecordRepo requestRecordRepo) {
        this.roomRequestRepo = roomRequestRepo;
        this.appUserRepo = appUserRepo;
        this.roomRepo = roomRepo;
        this.requestRecordRepo = requestRecordRepo;
    }

    @Transactional
    public void addRoomRequest(Long roomId, LocalDateTime start, LocalDateTime end) throws SQLException {
        User auth = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Room room = roomRepo.getOne(roomId);
        AppUser user = appUserRepo.findByUsername(auth.getUsername());
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setRequester(user);
        roomRequest.setRoom(room);
        roomRequest.setStartTime(Timestamp.valueOf(start));
        roomRequest.setEndTime(Timestamp.valueOf(end));
        ((Trainer) user).getRoomRequests().add(roomRequest);
        roomRequestRepo.save(roomRequest);
        requestRecordRepo.save(new RequestRecord("ROOM_REQ", roomRequest.getRequester().toString(),
                roomRequest.getRoom().toString(), LocalDate.now()));
    }

    @Transactional
    public List<RoomRequest> getAll() {
        return roomRequestRepo.findAll();
    }

    @Transactional
    public List<RoomRequest> getUnapprovedRequests() {
        return getAll().stream().filter(roomRequest -> (!roomRequest.getApprovedAdmin() | !roomRequest.getApprovedSecurity())).collect(toList());
    }

    @Transactional
    public void approveRequestAdmin(Long requestId) {
        RoomRequest one = roomRequestRepo.getOne(requestId);
        one.setApprovedAdmin(true);
    }

    @Transactional
    public void approveRequestSecurity(Long requestId) {
        RoomRequest one = roomRequestRepo.getOne(requestId);
        one.setApprovedSecurity(true);
    }

    @Transactional
    public void removeRequest(Long requestId) {
        RoomRequest matchedRoomRequest = roomRequestRepo.getOne(requestId);
        matchedRoomRequest.setRoom(null);
        matchedRoomRequest.setRequester(null);
        roomRequestRepo.delete(requestId);
    }
}
