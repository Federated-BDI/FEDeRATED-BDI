package nl.tno.federated.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * Specialized cargo, goods
 */

@Schema(name = "Goods", description = "Specialized cargo, goods")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-08-22T11:42:29.708+02:00[Europe/Amsterdam]")
public class Goods implements LoadEventInvolvedCargoInner {

  private Double grossMass;

  private Double grossVolume;

  private String goodsTypeCode;

  private Integer netMass;

  private Integer numberOfUnits;

  private String goodsDescription;

  private String digitalTwinType;

  private String digitalTwinID;

  /**
   * Default constructor
   * @deprecated Use {@link Goods#Goods(Double, Double, String, String, String)}
   */
  @Deprecated
  public Goods() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Goods(Double grossMass, Double grossVolume, String goodsTypeCode, String goodsDescription, String digitalTwinType) {
    this.grossMass = grossMass;
    this.grossVolume = grossVolume;
    this.goodsTypeCode = goodsTypeCode;
    this.goodsDescription = goodsDescription;
    this.digitalTwinType = digitalTwinType;
  }

  public Goods grossMass(Double grossMass) {
    this.grossMass = grossMass;
    return this;
  }

  /**
   * Get grossMass
   * @return grossMass
  */
  @NotNull 
  @Schema(name = "grossMass", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("grossMass")
  public Double getGrossMass() {
    return grossMass;
  }

  public void setGrossMass(Double grossMass) {
    this.grossMass = grossMass;
  }

  public Goods grossVolume(Double grossVolume) {
    this.grossVolume = grossVolume;
    return this;
  }

  /**
   * Get grossVolume
   * @return grossVolume
  */
  @NotNull 
  @Schema(name = "grossVolume", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("grossVolume")
  public Double getGrossVolume() {
    return grossVolume;
  }

  public void setGrossVolume(Double grossVolume) {
    this.grossVolume = grossVolume;
  }

  public Goods goodsTypeCode(String goodsTypeCode) {
    this.goodsTypeCode = goodsTypeCode;
    return this;
  }

  /**
   * Get goodsTypeCode
   * @return goodsTypeCode
  */
  @NotNull 
  @Schema(name = "goodsTypeCode", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("goodsTypeCode")
  public String getGoodsTypeCode() {
    return goodsTypeCode;
  }

  public void setGoodsTypeCode(String goodsTypeCode) {
    this.goodsTypeCode = goodsTypeCode;
  }

  public Goods netMass(Integer netMass) {
    this.netMass = netMass;
    return this;
  }

  /**
   * Get netMass
   * @return netMass
  */
  
  @Schema(name = "netMass", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("netMass")
  public Integer getNetMass() {
    return netMass;
  }

  public void setNetMass(Integer netMass) {
    this.netMass = netMass;
  }

  public Goods numberOfUnits(Integer numberOfUnits) {
    this.numberOfUnits = numberOfUnits;
    return this;
  }

  /**
   * Get numberOfUnits
   * @return numberOfUnits
  */
  
  @Schema(name = "numberOfUnits", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("numberOfUnits")
  public Integer getNumberOfUnits() {
    return numberOfUnits;
  }

  public void setNumberOfUnits(Integer numberOfUnits) {
    this.numberOfUnits = numberOfUnits;
  }

  public Goods goodsDescription(String goodsDescription) {
    this.goodsDescription = goodsDescription;
    return this;
  }

  /**
   * Get goodsDescription
   * @return goodsDescription
  */
  @NotNull 
  @Schema(name = "goodsDescription", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("goodsDescription")
  public String getGoodsDescription() {
    return goodsDescription;
  }

  public void setGoodsDescription(String goodsDescription) {
    this.goodsDescription = goodsDescription;
  }

  public Goods digitalTwinType(String digitalTwinType) {
    this.digitalTwinType = digitalTwinType;
    return this;
  }

  /**
   * Get digitalTwinType
   * @return digitalTwinType
  */
  @NotNull 
  @Schema(name = "digitalTwinType", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("digitalTwinType")
  public String getDigitalTwinType() {
    return digitalTwinType;
  }

  public void setDigitalTwinType(String digitalTwinType) {
    this.digitalTwinType = digitalTwinType;
  }

  public Goods digitalTwinID(String digitalTwinID) {
    this.digitalTwinID = digitalTwinID;
    return this;
  }

  /**
   * Get digitalTwinID
   * @return digitalTwinID
  */
  
  @Schema(name = "digitalTwinID", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("digitalTwinID")
  public String getDigitalTwinID() {
    return digitalTwinID;
  }

  public void setDigitalTwinID(String digitalTwinID) {
    this.digitalTwinID = digitalTwinID;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Goods goods = (Goods) o;
    return Objects.equals(this.grossMass, goods.grossMass) &&
        Objects.equals(this.grossVolume, goods.grossVolume) &&
        Objects.equals(this.goodsTypeCode, goods.goodsTypeCode) &&
        Objects.equals(this.netMass, goods.netMass) &&
        Objects.equals(this.numberOfUnits, goods.numberOfUnits) &&
        Objects.equals(this.goodsDescription, goods.goodsDescription) &&
        Objects.equals(this.digitalTwinType, goods.digitalTwinType) &&
        Objects.equals(this.digitalTwinID, goods.digitalTwinID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(grossMass, grossVolume, goodsTypeCode, netMass, numberOfUnits, goodsDescription, digitalTwinType, digitalTwinID);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Goods {\n");
    sb.append("    grossMass: ").append(toIndentedString(grossMass)).append("\n");
    sb.append("    grossVolume: ").append(toIndentedString(grossVolume)).append("\n");
    sb.append("    goodsTypeCode: ").append(toIndentedString(goodsTypeCode)).append("\n");
    sb.append("    netMass: ").append(toIndentedString(netMass)).append("\n");
    sb.append("    numberOfUnits: ").append(toIndentedString(numberOfUnits)).append("\n");
    sb.append("    goodsDescription: ").append(toIndentedString(goodsDescription)).append("\n");
    sb.append("    digitalTwinType: ").append(toIndentedString(digitalTwinType)).append("\n");
    sb.append("    digitalTwinID: ").append(toIndentedString(digitalTwinID)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

