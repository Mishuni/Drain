/*
 *******************************************************************
 *
 * Copyright 2015 Intel Corporation.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */
package com.ocfk.d2d.simpleserver;

import org.iotivity.base.OcException;
import org.iotivity.base.OcRepresentation;

/**
 * Light
 * <p/>
 * This class is used by SimpleClient to create an object representation of a remote light resource
 * and update the values depending on the server response
 */
public class Sensor {
    public static final String VALUE_KEY =  "value";


    boolean mValue;

    public Sensor() {
        mValue = false;
    }

    public boolean getValue(){
        return mValue;
    }

    public void setOcRepresentation(OcRepresentation rep) throws OcException {
        mValue = rep.getValue(VALUE_KEY);
    }

    public OcRepresentation getOcRepresentation() throws OcException {
        OcRepresentation rep = new OcRepresentation();
        rep.setValue(VALUE_KEY, mValue);
        return rep;
    }






    public void setValue(boolean value) {
        this.mValue = value;
    }

    @Override
    public String toString() {
        return "\t" + VALUE_KEY + ": " + mValue;
    }
}
