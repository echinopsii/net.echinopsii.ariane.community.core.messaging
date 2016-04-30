/**
 * Messaging - RabbitMQ Implementation
 * Message to be translated
 * Copyright (C) 8/25/14 echinopsii
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.echinopsii.ariane.community.messaging.rabbitmq;

import com.rabbitmq.client.BasicProperties;
import com.rabbitmq.client.Envelope;

public class Message {
    private Envelope        envelope = null;
    private BasicProperties properties = null;
    private byte[]          body = null;

    public Envelope getEnvelope() {
        return envelope;
    }

    public Message setEnvelope(Envelope envelope) {
        this.envelope = envelope;
        return this;
    }

    public BasicProperties getProperties() {
        return properties;
    }

    public Message setProperties(BasicProperties properties) {
        this.properties = properties;
        return this;
    }

    public byte[] getBody() {
        return body;
    }

    public Message setBody(byte[] body) {
        this.body = body;
        return this;
    }
}