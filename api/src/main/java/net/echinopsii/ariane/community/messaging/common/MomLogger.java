/**
 * Ariane Community Messaging
 * Mom Logger Implementation
 *
 * Copyright (C) 11/13/16 echinopsii
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
package net.echinopsii.ariane.community.messaging.common;

import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MomLogger implements net.echinopsii.ariane.community.messaging.api.MomLogger {

    private static volatile List<Long> itlEnabledPerThread = new ArrayList<>();
    public static final String FQCN = Logger.class.getName();

    private Logger log;

    private static int LEVEL_TRACE = 0;
    private static int LEVEL_DEBUG = 10;
    private static int LEVEL_INFO = 20;
    private static int LEVEL_WARN = 30;
    private static int LEVEL_ERROR = 40;

    public MomLogger(Class clazz) {
        log = LoggerFactory.getLogger(clazz);
    }

    @Override
    public String getName() {
        return log.getName();
    }


    @Override
    public boolean isTraceEnabled() {
        return (this.isTraceLevelEnabled() || log.isTraceEnabled());
    }

    @Override
    public void trace(String msg) {
        if (this.isTraceLevelEnabled()) this.log(null, FQCN, LEVEL_TRACE, msg, null, null);
        else log.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        log.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        log.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object[] argArray) {
        log.trace(format, argArray);
    }

    @Override
    public void trace(String msg, Throwable t) {
        this.log(null, FQCN, LEVEL_TRACE, msg, null, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return (this.isTraceLevelEnabled() || log.isTraceEnabled(marker));
    }

    @Override
    public void trace(Marker marker, String msg) {
        this.log(marker, FQCN, LEVEL_TRACE, msg, null, null);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        log.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        log.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object[] argArray) {
        log.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        this.log(marker, FQCN, LEVEL_TRACE, msg, null, t);
    }

    @Override
    public void debug(String msg) {
        if (this.isTraceLevelEnabled()) this.log(null, FQCN, LEVEL_DEBUG, msg, null, null);
        else log.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        log.trace(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        log.trace(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object[] argArray) {
        log.trace(format, argArray);
    }

    @Override
    public void debug(String msg, Throwable t) {
        this.log(null, FQCN, LEVEL_DEBUG, msg, null, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return (!this.isTraceLevelEnabled() && log.isDebugEnabled());
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return (!this.isTraceLevelEnabled() && log.isDebugEnabled(marker));
    }

    @Override
    public void debug(Marker marker, String msg) {
        this.log(marker, FQCN, LEVEL_DEBUG, msg, null, null);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        log.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        log.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object[] argArray) {
        log.debug(marker, format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        this.log(marker, FQCN, LEVEL_DEBUG, msg, null, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return !this.isTraceLevelEnabled() && log.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        if (this.isTraceLevelEnabled()) this.log(null, FQCN, LEVEL_INFO, msg, null, null);
        else log.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        log.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        log.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object[] argArray) {
        log.info(format, argArray);
    }

    @Override
    public void info(String msg, Throwable t) {
        this.log(null, FQCN, LEVEL_INFO, msg, null, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return !this.isTraceLevelEnabled() && log.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        this.log(marker, FQCN, LEVEL_INFO, msg, null, null);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        log.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        log.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object[] argArray) {
        log.info(marker, format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        this.log(marker, FQCN, LEVEL_TRACE, msg, null, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return (!this.isTraceLevelEnabled() && log.isWarnEnabled());
    }

    @Override
    public void warn(String msg) {
        if (this.isTraceLevelEnabled()) this.log(null, FQCN, LEVEL_WARN, msg, null, null);
        else log.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        log.warn(format, arg);
    }

    @Override
    public void warn(String format, Object[] argArray) {
        log.warn(format, argArray);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        log.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        this.log(null, FQCN, LEVEL_WARN, msg, null, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return (!this.isTraceLevelEnabled() && log.isWarnEnabled(marker));
    }

    @Override
    public void warn(Marker marker, String msg) {
        this.log(marker, FQCN, LEVEL_TRACE, msg, null, null);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        log.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        log.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object[] argArray) {
        log.warn(marker, format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        this.log(marker, FQCN, LEVEL_TRACE, msg, null, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return (!this.isTraceLevelEnabled() && log.isErrorEnabled());
    }

    @Override
    public void error(String msg) {
        if (this.isTraceLevelEnabled()) this.log(null, FQCN, LEVEL_ERROR, msg, null, null);
        else log.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        log.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        log.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object[] argArray) {
        log.error(format, argArray);
    }

    @Override
    public void error(String msg, Throwable t) {
        this.log(null, FQCN, LEVEL_ERROR, msg, null, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return (!this.isTraceLevelEnabled() && isErrorEnabled(marker));
    }

    @Override
    public void error(Marker marker, String msg) {
        this.log(marker, FQCN, LEVEL_TRACE, msg, null, null);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        log.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        log.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object[] argArray) {
        log.error(marker, format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        this.log(marker, FQCN, LEVEL_TRACE, msg, null, t);
    }

    @Override
    public net.echinopsii.ariane.community.messaging.api.MomLogger setTraceLevel(boolean isTraceLevelEnabled) {
        if (isTraceLevelEnabled && !itlEnabledPerThread.contains(Thread.currentThread().getId())) itlEnabledPerThread.add(Thread.currentThread().getId());
        else if (!isTraceLevelEnabled) itlEnabledPerThread.remove(Thread.currentThread().getId());
        return this;
    }

    @Override
    public void traceMessage(String opsName, Map<String, Object> message, String... ignoredFields) {
        Map<String, Object> tracedMessage = new HashMap<>(message);
        tracedMessage.remove(MomMsgTranslator.MSG_BODY);
        for (String ignoredField : ignoredFields)
            tracedMessage.remove(ignoredField);
        this.trace(opsName + " - " + tracedMessage.toString());
    }

    public boolean isTraceLevelEnabled() {
        long threadID = Thread.currentThread().getId();
        return (itlEnabledPerThread.contains(threadID));
    }

    @Override
    public void log(Marker marker, String fqcn, int level, String message, Object[] argArray, Throwable t) {
        if (this.isTraceLevelEnabled()) {
            if (log.isTraceEnabled()) log.trace("[ " + Thread.currentThread().getName() + " | MSG TRACE ]" + message);
            else if (log.isDebugEnabled()) log.debug("[ " + Thread.currentThread().getName() + " | MSG TRACE ]" + message);
            else if (log.isInfoEnabled()) log.info("[ " + Thread.currentThread().getName() + " | MSG TRACE ]" + message);
            else if (log.isWarnEnabled()) log.warn("[ " + Thread.currentThread().getName() + " | MSG TRACE ]" + message);
            else if (log.isErrorEnabled()) log.error("[ " + Thread.currentThread().getName() + " | MSG TRACE ]" + message);
        } else {
            if (log.isTraceEnabled()) log.trace(message);
            else if (log.isDebugEnabled()) log.debug(message);
            else if (log.isInfoEnabled()) log.info(message);
            else if (log.isWarnEnabled()) log.warn(message);
            else if (log.isErrorEnabled()) log.error(message);
        }
    }
}