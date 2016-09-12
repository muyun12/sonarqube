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
package org.sonar.server.qualityprofile.ws;

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.qualityprofile.QualityProfileDao;
import org.sonar.db.qualityprofile.QualityProfileDbTester;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.language.LanguageTesting;
import org.sonar.server.qualityprofile.QProfileFactory;
import org.sonar.server.qualityprofile.QProfileLoader;
import org.sonar.server.qualityprofile.QProfileLookup;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_DEFAULTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROFILE_NAME;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PROJECT_KEY;

public class SearchActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  // TODO remove mock
  private QProfileLoader profileLoader = mock(QProfileLoader.class);

  final DbClient dbClient = db.getDbClient();
  final DbSession dbSession = db.getSession();

  private QualityProfileDao qualityProfileDao = dbClient.qualityProfileDao();

  private Language xoo1;
  private Language xoo2;
  private WsActionTester ws;

  private QualityProfileDbTester qualityProfileDb;

  @Before
  public void setUp() {
    qualityProfileDb = new QualityProfileDbTester(db);

    xoo1 = LanguageTesting.newLanguage("xoo1");
    xoo2 = LanguageTesting.newLanguage("xoo2");

    Languages languages = new Languages(xoo1, xoo2);
    ws = new WsActionTester(new SearchAction(
      new SearchDataLoader(
        languages,
        new QProfileLookup(dbClient),
        profileLoader,
        new QProfileFactory(dbClient),
        dbClient,
        new ComponentFinder(dbClient)),
      languages));
  }

  @Test
  public void search_nominal() throws Exception {
    when(profileLoader.countAllActiveRules()).thenReturn(ImmutableMap.of(
      "sonar-way-xoo1-12345", 11L,
      "my-sonar-way-xoo2-34567", 33L));

    qualityProfileDao.insert(dbSession,
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1.getKey()).setName("Sonar way").setDefault(true),
      QualityProfileDto.createFor("sonar-way-xoo2-23456").setLanguage(xoo2.getKey()).setName("Sonar way"),
      QualityProfileDto.createFor("my-sonar-way-xoo2-34567").setLanguage(xoo2.getKey()).setName("My Sonar way").setParentKee("sonar-way-xoo2-23456"),
      QualityProfileDto.createFor("sonar-way-other-666").setLanguage("other").setName("Sonar way").setDefault(true));
    new ComponentDao().insert(dbSession,
      newProjectDto("project-uuid1"),
      newProjectDto("project-uuid2"));
    qualityProfileDao.insertProjectProfileAssociation("project-uuid1", "sonar-way-xoo2-23456", dbSession);
    qualityProfileDao.insertProjectProfileAssociation("project-uuid2", "sonar-way-xoo2-23456", dbSession);
    commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("SearchActionTest/search.json"));
  }

  @Test
  public void search_for_language() throws Exception {
    qualityProfileDao.insert(dbSession,
      QualityProfileDto.createFor("sonar-way-xoo1-12345").setLanguage(xoo1.getKey()).setName("Sonar way"));
    commit();

    String result = ws.newRequest().setParam("language", xoo1.getKey()).execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("SearchActionTest/search_xoo1.json"));
  }

  @Test
  public void search_for_project_qp() {
    QualityProfileDto qualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-12345")
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar way");
    QualityProfileDto qualityProfileOnXoo2 = QualityProfileDto.createFor("sonar-way-xoo2-12345")
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar way");
    QualityProfileDto anotherQualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-45678")
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Another way");
    ComponentDto project = newProjectDto("project-uuid");
    qualityProfileDb.insertQualityProfiles(qualityProfileOnXoo1, qualityProfileOnXoo2, anotherQualityProfileOnXoo1);
    qualityProfileDb.insertProjectWithQualityProfileAssociations(project, qualityProfileOnXoo1, qualityProfileOnXoo2);
    commit();

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.key())
      .execute().getInput();

    assertThat(result)
      .contains("sonar-way-xoo1-12345", "sonar-way-xoo2-12345")
      .doesNotContain("sonar-way-xoo1-45678");
  }

  @Test
  public void search_for_default_qp_with_profile_name() {
    QualityProfileDto qualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-12345")
      .setLanguage(xoo1.getKey())
      .setName("Sonar way")
      .setDefault(false);
    QualityProfileDto qualityProfileOnXoo2 = QualityProfileDto.createFor("sonar-way-xoo2-12345")
      .setLanguage(xoo2.getKey())
      .setName("Sonar way")
      .setDefault(true);
    QualityProfileDto anotherQualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-45678")
      .setLanguage(xoo1.getKey())
      .setName("Another way")
      .setDefault(true);
    qualityProfileDb.insertQualityProfiles(qualityProfileOnXoo1, qualityProfileOnXoo2, anotherQualityProfileOnXoo1);
    commit();

    String result = ws.newRequest()
      .setParam(PARAM_DEFAULTS, Boolean.TRUE.toString())
      .setParam(PARAM_PROFILE_NAME, "Sonar way")
      .execute().getInput();

    assertThat(result)
      .contains("sonar-way-xoo1-12345", "sonar-way-xoo2-12345")
      .doesNotContain("sonar-way-xoo1-45678");
  }

  @Test
  public void search_by_profile_name() {
    QualityProfileDto qualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-12345")
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar way");
    QualityProfileDto qualityProfileOnXoo2 = QualityProfileDto.createFor("sonar-way-xoo2-12345")
      .setLanguage(xoo2.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Sonar way");
    QualityProfileDto anotherQualityProfileOnXoo1 = QualityProfileDto.createFor("sonar-way-xoo1-45678")
      .setLanguage(xoo1.getKey())
      .setRulesUpdatedAtAsDate(new Date())
      .setName("Another way");
    ComponentDto project = newProjectDto("project-uuid");
    qualityProfileDb.insertQualityProfiles(qualityProfileOnXoo1, qualityProfileOnXoo2, anotherQualityProfileOnXoo1);
    dbClient.componentDao().insert(dbSession, project);
    commit();

    String result = ws.newRequest()
      .setParam(PARAM_PROJECT_KEY, project.key())
      .setParam(PARAM_PROFILE_NAME, "Sonar way")
      .execute().getInput();

    assertThat(result)
      .contains("sonar-way-xoo1-12345", "sonar-way-xoo2-12345")
      .doesNotContain("sonar-way-xoo1-45678");

  }

  private void commit() {
    dbSession.commit();
  }
}
