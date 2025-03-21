package ConfHandler.sevice;

import ConfHandler.model.dto.*;
import ConfHandler.model.entity.Conference;
import ConfHandler.model.entity.Event;
import ConfHandler.model.entity.Lecture;
import ConfHandler.model.entity.Session;
import ConfHandler.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DisplayConferenceService {

    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private LectureRepository lectureRepository;
    @Autowired
    private AttendeeRepository attendeeRepository;

    @Autowired
    private EventRepository eventRepository;


    @Autowired
    private ConferenceRepository conferenceRepository;
    public List<?> getDayOfConference(LocalDate date,UUID id) {

        List<Object> listOfAllEvents=new ArrayList<>();
        listOfAllEvents.addAll(sessionRepository.getSessionsByTimeStart(date).stream()
                .sorted(Comparator.comparing(Session::getTimeStart))

                .map(s -> SessionDto.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .duration(s.getDuration())
                        .city(s.getCity())
                        .roomNumber(s.getRoom_number())
                        .street(s.getStreet())
                        .building(s.getBuilding())
                        .eventList(getAllEventList(s,id))
                        .chairman(s.getChairmanList().isEmpty()?null:
                                s.getChairmanList().stream()
                                                .map(chairman ->
                                                        chairman.getParticipant().getName()+" "+chairman.getParticipant().getSurname())
                                                .collect(Collectors.joining(", ")))
                        .build()
                )
                .filter(sessionDto -> !sessionDto.getEventList().isEmpty())
                .toList());


        listOfAllEvents.addAll(
                id==null
                ?
                eventRepository.getEventsByDateWithoutSession(date)  .stream()
                        .map(
                                event -> {
                                    Lecture lecture = lectureRepository.getByEvent_Id(event.getId());
                                    return lecture == null ?
                                            new EventDto(event.getId(),event.getName(),event.getDuration(),event.getDescription(),event.getMenu() == null ? null : MenuDto.builder()
                                                    .header( event.getMenu().getHeader()).menuItems(event.getMenu().getItems()).build()) :
                                            new LectureDto(event.getId(),event.getName(),event.getDuration(),event.getDescription(),lecture.get_abstract(),lecture.getLecturersString(),lecture.getTopic(),
                                                    lecture.getChairmanList().isEmpty()?null:lecture.getChairmanList().stream()
                                                            .map(chairman ->
                                                                    chairman.getParticipant().getName()+" "+chairman.getParticipant().getSurname())
                                                            .collect(Collectors.joining(", ")));
                                })
                        .toList()
                :

                        attendeeRepository.getEventsByDateWithoutSessionOfUSer(date,id)
                .stream()
                .map(event -> new EventDto(event.getId(),event.getName(),event.getDuration(),event.getDescription(),event.getMenu() == null ? null : MenuDto.builder()
                        .header( event.getMenu().getHeader()).menuItems(event.getMenu().getItems()).build())).toList());

        listOfAllEvents.sort(Comparator.comparing(o -> {
            if (o instanceof SessionDto) {
                return ((SessionDto) o).getDuration();
            } else if (o instanceof LectureDto) {
                return ((LectureDto) o).getDuration();
            } else   {
                return ((EventDto) o).getDuration();
            }

        }));


        return listOfAllEvents;
    }


    private List<EventDto> getAllEventList(Session session,UUID id ) {
        return session.getEventList().stream()
                .filter(event -> id == null || attendeeRepository.getIdsOfUserEvents(id).contains(event.getId()))
                .sorted(Comparator.comparing(Event::getTimeStart))
                .map(event -> {
                    Lecture lecture = lectureRepository.getByEvent_Id(event.getId());
                    return lecture == null ?
                            new EventDto(event.getId(),event.getName(),event.getDuration(),event.getDescription(),event.getMenu() == null ? null : MenuDto.builder()
                                    .header( event.getMenu().getHeader()).menuItems(event.getMenu().getItems()).build()):
                            new LectureDto(event.getId(),event.getName(),event.getDuration(),event.getDescription(),lecture.get_abstract(),lecture.getLecturersString(),lecture.getTopic(),
                                    lecture.getChairmanList().isEmpty()?null: lecture.getChairmanList().stream()
                                            .map(chairman ->
                                                    chairman.getParticipant().getName()+" "+chairman.getParticipant().getSurname())
                                            .collect(Collectors.joining(", ")));
                })
                .toList();
    }

    public ConferenceInfoDto getConferenceInfo() {
        return conferenceRepository.getConferenceInfo().orElseThrow(()->new NullPointerException("conference not found"));
    }

    public MetadataDto getMetadata() {
        Conference conference = conferenceRepository.getConference().orElseThrow(()->new NullPointerException("conference not found"));
        return   MetadataDto.builder()
                .contactEmail(conference.getContactEmail())
                .contactCellNumber(conference.getContactCellNumber())
                .contactWebsite(conference.getContactWebsite())
                .contactLandlineNumber(conference.getContactLandlineNumber()).build()
                ;

    }
}
