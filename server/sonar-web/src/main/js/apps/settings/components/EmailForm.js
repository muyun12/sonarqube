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
import React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { sendTestEmail } from '../../../api/settings';
import { parseError } from '../../code/utils';

export default class EmailForm extends React.Component {
  constructor (props) {
    super(props);
    this.state = {
      recipient: window.SS.userEmail,
      subject: translate('email_configuration.test.subject'),
      message: translate('email_configuration.test.message_text'),
      loading: false,
      success: false,
      error: null
    };
  }

  handleFormSubmit (e) {
    e.preventDefault();
    this.setState({ success: false, error: null, loading: true });
    const { recipient, subject, message } = this.state;
    sendTestEmail(recipient, subject, message).then(
        () => this.setState({ success: true, loading: false }),
        error => parseError(error).then(message => this.setState({ error: message, loading: false }))
    );
  }

  render () {
    return (
        <div className="huge-spacer-top">
          <h3 className="spacer-bottom">{translate('email_configuration.test.title')}</h3>

          <form className="display-inline-block" onSubmit={e => this.handleFormSubmit(e)}>
            {this.state.success && (
                <div className="alert alert-success">
                  {translateWithParameters('email_configuration.test.email_was_sent_to_x', this.state.recipient)}
                </div>
            )}

            {this.state.error != null && (
                <div className="alert alert-danger">
                  {this.state.error}
                </div>
            )}

            <div className="modal-field">
              <label htmlFor="test-email-to">
                {translate('email_configuration.test.to_address')}
                <em className="mandatory">*</em>
              </label>
              <input
                  id="test-email-to"
                  type="email"
                  required
                  value={this.state.recipient}
                  disabled={this.state.loading}
                  onChange={e => this.setState({ recipient: e.target.value })}/>
            </div>
            <div className="modal-field">
              <label htmlFor="test-email-subject">
                {translate('email_configuration.test.subject')}
              </label>
              <input
                  id="test-email-subject"
                  type="text"
                  value={this.state.subject}
                  disabled={this.state.loading}
                  onChange={e => this.setState({ subject: e.target.value })}/>
            </div>
            <div className="modal-field">
              <label htmlFor="test-email-message">
                {translate('email_configuration.test.message')}
                <em className="mandatory">*</em>
              </label>
              <textarea
                  id="test-email-title"
                  required
                  rows="5"
                  value={this.state.message}
                  disabled={this.state.loading}
                  onChange={e => this.setState({ message: e.target.value })}/>
            </div>

            <div className="text-right">
              {this.state.loading && <i className="spacer-right spinner"/>}
              <button disabled={this.state.loading}>{translate('email_configuration.test.send')}</button>
            </div>
          </form>
        </div>
    );
  }
}
