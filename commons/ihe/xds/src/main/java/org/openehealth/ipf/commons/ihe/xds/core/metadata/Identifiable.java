/*
 * Copyright 2009 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openehealth.ipf.commons.ihe.xds.core.metadata;

import ca.uhn.hl7v2.model.v25.datatype.CX;
import org.openehealth.ipf.commons.ihe.xds.core.metadata.jaxbadapters.AssigningAuthorityAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Objects;

/**
 * Represents an ID.
 * <p>
 * This class is derived from an HL7v2 CX data type. The XDS profile
 * limits the data type to two of its components (CX.1 and CX.4).
 * <p>
 * All members of this class are allowed to be <code>null</code>. When transforming
 * to HL7 this indicates that the values are empty. Trailing empty values are 
 * removed from the HL7 string.
 * @author Jens Riemschneider
 * @author Dmytro Rud
 */
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlType(name = "Identifiable", propOrder = {"id", "assigningAuthority"})
public class Identifiable extends Hl7v2Based<CX> {
    private static final long serialVersionUID = -3392755556068006520L;

    /**
     * Constructs an identifiable.
     */
    public Identifiable() {
        super(new CX(MESSAGE));
    }


    /**
     * Constructs an identifiable.
     */
    public Identifiable(CX cx) {
        super(cx);
    }


    /**
     * Constructs an identifiable.
     * @param id
     *          the value of the id (CX.1).
     * @param assigningAuthority
     *          the assigning authority (CX.4).
     */
    public Identifiable(String id, AssigningAuthority assigningAuthority) {
        this();
        setId(id);
        setAssigningAuthority(assigningAuthority);
    }

    /**
     * @return the value of the id (CX.1).
     */
    @XmlAttribute(name = "extension")
    public String getId() {
        return getHapiObject().getCx1_IDNumber().getValue();
    }

    /**
     * @param id
     *          the value of the id (CX.1).
     */
    public void setId(String id) {
        setValue(getHapiObject().getCx1_IDNumber(), id);
    }

    /**
     * @return the assigning authority (CX.4).
     */
    @XmlAttribute(name = "root")
    @XmlJavaTypeAdapter(value = AssigningAuthorityAdapter.class)
    public AssigningAuthority getAssigningAuthority() {
        AssigningAuthority assigningAuthority = new AssigningAuthority(getHapiObject().getCx4_AssigningAuthority());
        return assigningAuthority.isEmpty() ? null : assigningAuthority;
    }

    /**
     * @param assigningAuthority
     *          the assigning authority (CX.4).
     */
    public void setAssigningAuthority(AssigningAuthority assigningAuthority) {
        setAssigningAuthority(assigningAuthority, getHapiObject().getCx4_AssigningAuthority());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifiable that = (Identifiable) o;
        return Objects.equals(getAssigningAuthority(), that.getAssigningAuthority()) &&
                Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAssigningAuthority(), getId());
    }

    @Override
    public String toString() {
        return "Identifiable(" +
                "id=" + getId() +
                ", assigningAuthority=" + getAssigningAuthority() +
                ')';
    }
}
