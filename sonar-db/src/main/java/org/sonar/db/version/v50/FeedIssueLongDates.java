/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.version.v50;

import java.sql.SQLException;
import java.util.Date;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;

public class FeedIssueLongDates extends BaseDataChange {

  private final System2 system;

  public FeedIssueLongDates(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    final long now = system.now();

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT i.id, i.created_at, i.updated_at FROM issues i WHERE created_at_ms IS NULL");
    massUpdate.update("UPDATE issues SET created_at_ms=?, updated_at_ms=? WHERE id=?");
    massUpdate.rowPluralName("issues");
    massUpdate.execute((row, update) -> {
      Long id = row.getNullableLong(1);
      Date createdAt = row.getNullableDate(2);
      Date updatedAt = row.getNullableDate(3);

      if (createdAt == null) {
        update.setLong(1, now);
      } else {
        update.setLong(1, Math.min(now, createdAt.getTime()));
      }
      if (updatedAt == null) {
        update.setLong(2, now);
      } else {
        update.setLong(2, Math.min(now, updatedAt.getTime()));
      }
      update.setLong(3, id);
      return true;
    });
  }

}
