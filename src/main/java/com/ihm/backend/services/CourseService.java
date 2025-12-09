package com.ihm.backend.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ihm.backend.DTO.responses.CourseResponse;
import com.ihm.backend.DTO.requests.CourseCreateRequestDTO;
import com.ihm.backend.DTO.requests.CourseUpdateRequestDTO;
import com.ihm.backend.mappers.CourseMapper;
import com.ihm.backend.repositories.CourseRepository;
import com.ihm.backend.repositories.UserRepository;

import jakarta.mail.Multipart;

import com.ihm.backend.entities.*;
import com.ihm.backend.enums.CourseStatus;
@Service
public class CourseService {
    @Autowired
    private  CourseMapper courseMapper;
    @Autowired
    private  CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;
    //create a course
   public CourseResponse createCourse(CourseCreateRequestDTO dto,Integer authorId) throws Exception{
    Course course=courseMapper.toEntity(dto);
    User author=userRepository.findById(authorId).orElseThrow(()->new Exception("Teacher does not exists"));
    course.setAuthor(author);
    course =courseRepository.save(course);
    return courseMapper.toResponse(course);
   }
   //get all courses for a particular author

   public List<CourseResponse> getAllCoursesForTeacher(Integer authorId)throws Exception{
    User author=userRepository.findById(authorId).orElseThrow(()->new Exception("Teacher does not exists"));
    return courseMapper.toResponse(courseRepository.findByAuthor(author));
   }
   //update course
   public CourseResponse updateCourse(Integer courseId,CourseUpdateRequestDTO request) throws Exception{
    Course course=courseRepository.findById(courseId)
                    .orElseThrow(()->new Exception("Course does not exist"));

    courseMapper.updateEntityFromDto(request, course);
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
    
}
