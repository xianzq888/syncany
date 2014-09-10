/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.plugins.transfer;

import org.reflections.Reflections;
import org.syncany.plugins.StorageException;
import org.syncany.plugins.StorageTestResult;
import org.syncany.plugins.annotations.PluginSettings;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements basic functionality of a {@link TransferManager} which
 * can be implemented sub-classes.
 *
 * @author Philipp C. Heckel <philipp.heckel@gmail.com>
 * @author Christian Roth <christian.roth@port17.de>
 */
public abstract class AbstractTransferManager implements TransferManager {
	private static final Logger logger = Logger.getLogger(AbstractTransferManager.class.getSimpleName());
	private TransferSettings settings;

	public AbstractTransferManager(TransferSettings settings) {
		this.settings = settings;
	}

  public final <T extends TransferSettings> T getConnection() {
    Reflections reflections = new Reflections("org.syncany");
    try {
      for (Class<?> annotatedClass : reflections.getTypesAnnotatedWith(PluginSettings.class)) {
        if (annotatedClass.getAnnotationsByType(PluginSettings.class)[0].value().getName().equals(settings.getClass().getName())) {
          return (T) annotatedClass.getAnnotationsByType(PluginSettings.class)[0].value().cast(settings);
        }
      }
    }
    catch (Exception e) {
      logger.log(Level.SEVERE, "Unable to read type: No TransferPlugin is defined for these settings", e);
    }
    return null;

  }

	// TODO [low] This should be in AbstractTransferManager (or any other central place), this should use the Syncany cache folder
	protected File createTempFile(String name) throws IOException {
		return File.createTempFile(String.format("temp-%s-", name), ".tmp");
	}

	@Override
	public StorageTestResult test(boolean testCreateTarget) {
		logger.log(Level.INFO, "Performing storage test TM.test() ...");
		StorageTestResult result = new StorageTestResult();

		try {
			logger.log(Level.INFO, "- Running connect() ...");
			connect();

			result.setTargetExists(testTargetExists());
			result.setTargetCanWrite(testTargetCanWrite());
			result.setRepoFileExists(testRepoFileExists());

			if (result.isTargetExists()) {
				result.setTargetCanCreate(true);
			}
			else {
				if (testCreateTarget) {
					result.setTargetCanCreate(testTargetCanCreate());
				}
				else {
					result.setTargetCanCreate(false);
				}
			}

			result.setTargetCanConnect(true);
		}
		catch (StorageException e) {
			result.setTargetCanConnect(false);
			result.setException(e);

			logger.log(Level.INFO, "-> Testing storage failed. Returning " + result, e);
		}
		finally {
			try {
				disconnect();
			}
			catch (StorageException e) {
				// Don't care
			}
		}

		return result;
	}
}
