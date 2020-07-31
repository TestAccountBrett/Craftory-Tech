/*******************************************************************************
 * Copyright (c) 2020. BrettSaunders & Craftory Team - All Rights Reserved
 *
 * This file is part of Craftory.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential
 *
 * File Author: Brett Saunders & Matty Jones
 ******************************************************************************/

package tech.brettsaunders.craftory.persistence.adapters;


import de.tr7zw.changeme.nbtapi.NBTCompound;
import lombok.NoArgsConstructor;
import tech.brettsaunders.craftory.persistence.PersistenceStorage;

@NoArgsConstructor
public class StringAdapter implements DataAdapter<String> {

    @Override
    public void store(PersistenceStorage persistenceStorage, String value, NBTCompound nbtCompound) {
        nbtCompound.setString("data", value);
    }

    @Override
    public String parse(PersistenceStorage persistenceStorage, Object parentObject, NBTCompound nbtCompound) {
        if (!nbtCompound.hasKey("data")) {
            return null;
        }
        return nbtCompound.getString("data");
    }
}
