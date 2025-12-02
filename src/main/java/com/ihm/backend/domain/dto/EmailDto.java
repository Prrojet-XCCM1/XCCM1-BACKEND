package cm.enspy.xccm.domain.dto;

import cm.enspy.xccm.domain.enums.EmailTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO pour l'envoi d'emails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDto {
    private String to;
    private String subject;
    private String body;
    private EmailTemplate template;
    private Map<String, Object> templateVariables;
    private boolean isHtml;
}