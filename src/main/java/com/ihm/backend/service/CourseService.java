package com.ihm.backend.service;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ihm.backend.dto.response.CourseResponse;
import com.ihm.backend.dto.response.EnrichedCourseResponse;
import com.ihm.backend.dto.request.CourseCreateRequest;
import com.ihm.backend.dto.request.CourseUpdateRequest;
import com.ihm.backend.exception.ResourceNotFoundException;
import com.ihm.backend.mappers.CourseMapper;
import com.ihm.backend.repository.CourseRepository;
import com.ihm.backend.repository.EnrollmentRepository;
import com.ihm.backend.repository.UserRepository;

import java.util.UUID;
import com.ihm.backend.entity.*;
import com.ihm.backend.enums.CourseStatus;
@Service
public class CourseService {
    @Autowired
    private  CourseMapper courseMapper;
    @Autowired
    private  CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    //create a course
   public CourseResponse createCourse(CourseCreateRequest dto,UUID authorId) throws Exception{
    Course course=courseMapper.toEntity(dto);
    User author=userRepository.findById(authorId).orElseThrow(()->new Exception("Teacher does not exists"));
    course.setAuthor(author);
    course =courseRepository.save(course);
    return courseMapper.toResponse(course);
   }
   //get all courses for a particular author

   public List<CourseResponse> getAllCoursesForTeacher(UUID authorId)throws Exception{
    User author=userRepository.findById(authorId).orElseThrow(()->new Exception("Teacher does not exists"));
    return courseMapper.toResponse(courseRepository.findByAuthor(author));
   }
   //update course
   public CourseResponse updateCourse(Integer courseId,CourseUpdateRequest request) throws Exception{
    Course course=courseRepository.findById(courseId)
                    .orElseThrow(()->new Exception("Course does not exist"));

    courseMapper.updateEntity(request, course);
    course=courseRepository.save(course);
    return courseMapper.toResponse(course);


   }

   //get all courses
   public List<CourseResponse> getAllCourses(){
    return courseMapper.toResponse(courseRepository.findByStatus(CourseStatus.PUBLISHED));
   }
   //delete course
   public void deleteCourse(Integer courseId) throws Exception{
    Course course=courseRepository.findById(courseId).orElseThrow(()->new Exception("Course does not exist"));
    courseRepository.delete(course);
   }

   //changeState of Course
   public CourseResponse changeCourseStatus(CourseStatus courseStatus,Integer courseId) throws Exception{
    Course course=courseRepository.findById(courseId)
                    .orElseThrow(()->new Exception("Course does not exist"));
    course.setStatus(courseStatus);
    courseRepository.save(course);
    return courseMapper.toResponse(course);
   }
   public List<CourseResponse> getCoursesByStatusForAuthor(Integer authorId,CourseStatus courseStatus) throws Exception{
    List<Course> courses=courseRepository.findByStatus(courseStatus);
    List<Course> result = courses.stream().filter(course-> course.getAuthor().getId().equals(authorId)).collect(Collectors.toList());
    
    return courseMapper.toResponse(result);
   }

    public CourseResponse uploadCoverImage(Integer courseId,MultipartFile image) throws Exception{


        Course course=courseRepository.findById(courseId).
        orElseThrow(()->new Exception() );

   

     String fileName = image.getOriginalFilename();
    Path uploadDir = Paths.get("uploads");
    Files.createDirectories(uploadDir);

    Path filePath = uploadDir.resolve(fileName);
    Files.write(filePath, image.getBytes());

    // Save path accessible from frontend
    String urlPath = "/uploads/" + fileName;
    course.setCoverImage(urlPath);
    courseRepository.save(course);
    return courseMapper.toResponse(course);


    }
    
    /**
     * Valide que l'enseignant est propriétaire du cours
     */
    public void validateOwnership(Integer courseId, UUID teacherId) throws Exception {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
        
        if (!course.getAuthor().getId().equals(teacherId)) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres cours");
        }
    }
    
    /**
     * Récupère tous les cours publiés enrichis avec l'enrôlement de l'utilisateur si applicable
     */
    public List<EnrichedCourseResponse> getEnrichedCourses(UUID userId) {
        List<Course> publishedCourses = courseRepository.findByStatus(CourseStatus.PUBLISHED);
        
        return publishedCourses.stream()
            .map(course -> {
                Enrollment enrollment = null;
                if (userId != null) {
                    enrollment = enrollmentRepository.findByCourse_IdAndUser_Id(
                        course.getId(), userId
                    ).orElse(null);
                }
                return EnrichedCourseResponse.fromCourse(course, enrollment);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère un cours enrichi avec l'enrôlement de l'utilisateur si applicable
     */
    public EnrichedCourseResponse getEnrichedCourse(Integer courseId, UUID userId) throws Exception {
        Course course = courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Cours non trouvé"));
        
        Enrollment enrollment = null;
        if (userId != null) {
            enrollment = enrollmentRepository.findByCourse_IdAndUser_Id(
                courseId, userId
            ).orElse(null);
        }
        
        return EnrichedCourseResponse.fromCourse(course, enrollment);
    }
}
