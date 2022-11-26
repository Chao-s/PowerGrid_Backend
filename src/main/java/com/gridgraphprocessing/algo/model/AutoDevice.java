package com.gridgraphprocessing.algo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "decloud_autodevice_account")
@Entity(name = "decloud_autodevice_account")
public class AutoDevice {
    @Id
    String id;//理论上可以是BigDecimal
    String name;
    @Column(name = "regionname")
    String regionName;
    @NotNull(message = "PSRID must not be null.")
    String psrid;
    @Column(name = "is_auto")
    Integer isAuto;//理论上可以是BigDecimal
    @NotNull(message = "Timestamp must not be null.")
    @Column(name = "update_time")
    Timestamp updateTime;
    @Column(name = "rese_string1")
    String reseString1;
    @Column(name = "rese_string2")
    String reseString2;
    @Column(name = "rese_long1")
    BigDecimal reseLong1;
    @Column(name = "rese_long2")
    BigDecimal reseLong2;
    @Column(name = "rese_int1")
    BigDecimal reseInt1;
    @Column(name = "rese_int2")
    BigDecimal reseInt2;
}
