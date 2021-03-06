/*
 * Copyright 2011 the original author or authors.
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
package org.openehealth.ipf.commons.ihe.hl7v3

import groovy.util.slurpersupport.GPathResult
import org.openehealth.ipf.commons.ihe.core.atna.AuditStrategySupport
import org.openhealthtools.ihe.atna.auditor.codes.rfc3881.RFC3881EventCodes.RFC3881EventOutcomeCodes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Dmytro Rud
 */
abstract class Hl7v3AuditStrategy extends AuditStrategySupport<Hl7v3AuditDataset> {
    protected static final transient Logger LOG = LoggerFactory.getLogger(Hl7v3AuditStrategy.class)

    Hl7v3AuditStrategy(boolean serverSide) {
        super(serverSide)
    }


    @Override
    public Hl7v3AuditDataset createAuditDataset() {
        return new Hl7v3AuditDataset(serverSide)
    }


    /**
     * Returns ATNA response code on the basis of Acknowledgement.typeCode
     * of the HL7 v3 output message:
     * <ul>
     *   <li> when the output message cannot be parsed -- "major failure",
     *   <li> when the typeCode is missing -- "major failure",
     *   <li> when the typeCode is 'AA' or 'CA' -- "success",
     *   <li> in all other cases ('AE' and 'QE') -- "serious failure".
     * </ul>
     *
     * @param gpath
     *      response message as {@link GPathResult}.
     */
    @Override
    RFC3881EventOutcomeCodes getEventOutcomeCode(Object gpath) {
        try {
            String code = gpath.acknowledgement[0].typeCode.@code.text()
            if (! code) {
                // code not found -- bad XML
                return RFC3881EventOutcomeCodes.MAJOR_FAILURE
            }
            return ((code == 'AA') || (code == 'CA')) ?
                    RFC3881EventOutcomeCodes.SUCCESS :
                    RFC3881EventOutcomeCodes.SERIOUS_FAILURE

        } catch (Exception e) {
            LOG.error('Exception in audit strategy', e)
            return RFC3881EventOutcomeCodes.MAJOR_FAILURE
        }
    }


    @Override
    boolean enrichAuditDatasetFromResponse(Hl7v3AuditDataset auditDataset, Object response) {
        auditDataset.eventOutcomeCode = getEventOutcomeCode(slurp(response))
        auditDataset.eventOutcomeCode == RFC3881EventOutcomeCodes.SUCCESS
    }


    static void addPatientIds(GPathResult source, Set<String> target) {
        for (node in source) {
            target << Hl7v3Utils.iiToCx(node)
        }
    }


    static GPathResult slurp(Object message) {
        return (message instanceof GPathResult) ? message : Hl7v3Utils.slurp((String) message)
    }
}
