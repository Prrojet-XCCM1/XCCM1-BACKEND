package com.ihm.backend.services;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import com.ihm.backend.DTO.requests.CourseCreateRequestDTO;
import com.ihm.backend.DTO.requests.CourseUpdateRequestDTO;
import com.ihm.backend.DTO.responses.CourseResponse;
import com.ihm.backend.entities.*;
import com.ihm.backend.enums.CourseStatus;
import com.ihm.backend.mappers.CourseMapper;
import com.ihm.backend.repositories.CourseRepository;
import com.ihm.backend.repositories.UserRepository;

@SpringBootTest
public class CourseServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CourseService courseService;

    private User mockTeacher;
    private Course mockCourse;
    private CourseResponse mockResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        mockTeacher = new User();
        mockTeacher.setId(10);

        mockCourse = new Course();
        mockCourse.setId(1);

        mockResponse = new CourseResponse();
        mockResponse.setId(1);
    }

    // -------------------------------------------------------------
    // CREATE COURSE
    // -------------------------------------------------------------
    @Test
    void testCreateCourse_success() throws Exception {
        CourseCreateRequestDTO dto = new CourseCreateRequestDTO();

        when(courseMapper.toEntity(dto)).thenReturn(mockCourse);
        when(userRepository.findById(10)).thenReturn(Optional.of(mockTeacher));
        when(courseRepository.save(mockCourse)).thenReturn(mockCourse);
        when(courseMapper.toResponse(mockCourse)).thenReturn(mockResponse);

        CourseResponse response = courseService.createCourse(dto, 10);

        assertNotNull(response);
        assertEquals(1, response.getId());
        verify(courseRepository).save(mockCourse);
    }

    @Test
    void testCreateCourse_teacherNotFound() {
        CourseCreateRequestDTO dto = new CourseCreateRequestDTO();

        when(courseMapper.toEntity(dto)).thenReturn(mockCourse);
        when(userRepository.findById(10)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> courseService.createCourse(dto, 10));

        assertEquals("Teacher does not exists", ex.getMessage());
    }

    // -------------------------------------------------------------
    // GET ALL COURSES FOR TEACHER
    // -------------------------------------------------------------
    @Test
    void testGetAllCoursesForTeacher_success() throws Exception {

        List<Course> courseList = List.of(mockCourse);
        List<CourseResponse> responseList = List.of(mockResponse);

        when(userRepository.findById(10)).thenReturn(Optional.of(mockTeacher));
        when(courseRepository.findByAuthor(mockTeacher)).thenReturn(courseList);
        when(courseMapper.toResponse(courseList)).thenReturn(responseList);

        List<CourseResponse> result = courseService.getAllCoursesForTeacher(10);

        assertEquals(1, result.size());
        verify(courseRepository).findByAuthor(mockTeacher);
    }

    @Test
    void testGetAllCoursesForTeacher_teacherNotFound() {
        when(userRepository.findById(10)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> courseService.getAllCoursesForTeacher(10));

        assertEquals("Teacher does not exists", ex.getMessage());
    }

    // -------------------------------------------------------------
    // UPDATE COURSE
    // -------------------------------------------------------------
    @Test
    void testUpdateCourse_success() throws Exception {
        CourseUpdateRequestDTO updateDto = new CourseUpdateRequestDTO();

        when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
        when(courseRepository.save(mockCourse)).thenReturn(mockCourse);
        when(courseMapper.toResponse(mockCourse)).thenReturn(mockResponse);

        CourseResponse response = courseService.updateCourse(1, updateDto);

        assertNotNull(response);
        assertEquals(1, response.getId());
        verify(courseMapper).updateEntityFromDto(updateDto, mockCourse);
    }

    @Test
    void testUpdateCourse_notFound() {
        CourseUpdateRequestDTO updateDto = new CourseUpdateRequestDTO();

        when(courseRepository.findById(1)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> courseService.updateCourse(1, updateDto));

        assertEquals("Course does not exist", ex.getMessage());
    }

    // -------------------------------------------------------------
    // GET ALL COURSES
    // -------------------------------------------------------------
    @Test
    void testGetAllCourses() {
        List<Course> courseList = List.of(mockCourse);
        List<CourseResponse> responseList = List.of(mockResponse);

        when(courseRepository.findAll()).thenReturn(courseList);
        when(courseMapper.toResponse(courseList)).thenReturn(responseList);

        List<CourseResponse> result = courseService.getAllCourses();

        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------
    // DELETE COURSE
    // -------------------------------------------------------------
    @Test
    void testDeleteCourse_success() throws Exception {
        when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));

        courseService.deleteCourse(1);

        verify(courseRepository).delete(mockCourse);
    }

    @Test
    void testDeleteCourse_notFound() {
        when(courseRepository.findById(1)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> courseService.deleteCourse(1));

        assertEquals("Course does not exist", ex.getMessage());
    }

    // -------------------------------------------------------------
    // CHANGE COURSE STATUS
    // -------------------------------------------------------------
    @Test
    void testChangeCourseStatus_success() throws Exception {
        when(courseRepository.findById(1)).thenReturn(Optional.of(mockCourse));
        when(courseRepository.save(mockCourse)).thenReturn(mockCourse);
        when(courseMapper.toResponse(mockCourse)).thenReturn(mockResponse);

        CourseResponse result = courseService.changeCourseStatus(CourseStatus.PUBLISHED, 1);

        assertEquals(1, result.getId());
        assertEquals(CourseStatus.PUBLISHED, mockCourse.getStatus());
    }

    @Test
    void testChangeCourseStatus_notFound() {
        when(courseRepository.findById(1)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class,
                () -> courseService.changeCourseStatus(CourseStatus.PUBLISHED, 1));

        assertEquals("Course does not exist", ex.getMessage());
    }
}
