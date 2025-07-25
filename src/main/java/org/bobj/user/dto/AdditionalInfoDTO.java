// AdditionalInfoDTO.java
package org.bobj.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class AdditionalInfoDTO {
    private String name;
    private String ssn;
    private String phone;
    private String bankCode;
    private String accountNumber;
}