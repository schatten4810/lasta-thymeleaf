/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.thymeleaf.processor;

import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.Arguments;
import org.thymeleaf.Configuration;
import org.thymeleaf.dom.Element;
import org.thymeleaf.processor.AttributeNameProcessorMatcher;
import org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor;
import org.thymeleaf.processor.attr.AbstractIterationAttrProcessor;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.util.Validate;

/**
 * Processor for Option Attribute of Select Tag with Classification Definition.
 * <pre>
 * Usage:
 *   &lt;select <b>la:property="status"</b>&gt;
 *     &lt;option&gt;&lt;/option&gt;
 *     &lt;option <b>la:optionCls="'MemberStatus'"</b>&gt;&lt;/option&gt;
 *   &lt;/select&gt;
 *
 * This means is :
 *   &lt;select name="status"&gt;
 *     &lt;option&gt;&lt;/option&gt;
 *     &lt;option <b>th:each="opdef : ${#cdef.values('MemberStatus')}" th:value="${#cdef.code(opdef)}" th:text="${#cdef.alias(opdef)}"</b> th:selected="${opdef} == ${status}"&gt;&lt;/option&gt;
 *   &lt;/select&gt;
 *
 * The result of processing this example will be as expected.
 *   &lt;select name="status"&gt;
 *     &lt;option&gt;&lt;/option&gt;
 *     &lt;option selected="selected" value="FML"&gt;Formalized&lt;/option&gt;
 *     &lt;option value="WDL"&gt;Withdrawal&lt;/option&gt;
 *     &lt;option value="PRV"&gt;Provisional&lt;/option&gt;
 *   &lt;/select&gt;
 *
 * 2.If Custom text use.("prod" is iteration variable)
 *   &lt;select la:proparty="status"&gt;
 *     &lt;option&gt;&lt;/option&gt;
 *     &lt;option <b>la:optionCls="prod : 'MemberStatus'"</b> th:text="${#cdef.code(prod)} + ' : ' + ${#cdef.alias(prod)}"&gt;&lt;/option&gt;
 *   &lt;/select&gt;
 *
 * The result of processing this example will be as expected.
 *   &lt;select name="status"&gt;
 *     &lt;option&gt;&lt;/option&gt;
 *     &lt;option selected="selected" value="FML"&gt;FML : Formalized&lt;/option&gt;
 *     &lt;option value="WDL"&gt;WDL : Withdrawal&lt;/option&gt;
 *     &lt;option value="PRV"&gt;PRV : Provisional&lt;/option&gt;
 *   &lt;/select&gt;
 * </pre>
 * @author schatten
 */
public class OptionClsAttrProcessor extends AbstractAttributeModifierAttrProcessor {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final String OPTION_CLS_ATTRIBUTE_NAME = "optionCls";
    private static final String OPTION_CLS_ELEMENT_TARGET = "option";
    private static final String DEFAULT_ITERATION_VALUE = "opdef";

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    protected OptionClsAttrProcessor() {
        super(new AttributeNameProcessorMatcher(OPTION_CLS_ATTRIBUTE_NAME, OPTION_CLS_ELEMENT_TARGET));
    }

    // ===================================================================================
    //                                                                          Implements
    //                                                                          ==========
    /**
     * {@inheritDoc}
     * @see org.thymeleaf.processor.AbstractProcessor#getPrecedence()
     */
    @Override
    public int getPrecedence() {
        return 200;
    }

    /**
     * {@inheritDoc}
     * @see org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor#getModifiedAttributeValues(org.thymeleaf.Arguments, org.thymeleaf.dom.Element, java.lang.String)
     */
    @Override
    protected Map<String, String> getModifiedAttributeValues(Arguments arguments, Element element, String attributeName) {

        IterationSpec spec = getIterationSpec(arguments, element, attributeName);

        Map<String, String> map = new HashMap<String, String>();
        map.put("th:each", String.format("%s, %s : ${#cdef.values('%s')}", spec.getIterVarName(), spec.getStatusVarName() ,spec.getClassificationName()));
        if (!element.hasNormalizedAttribute(StandardDialect.PREFIX, "value")) {
            map.put("th:value", String.format("${#cdef.code(%s)}",spec.getIterVarName()));
        }
        if (!element.hasNormalizedAttribute(StandardDialect.PREFIX, "text")) {
            map.put("th:text", String.format("${#cdef.alias(%s)}",spec.getIterVarName()));
        }
        if (!element.hasNormalizedAttribute(StandardDialect.PREFIX, "selected")) {
            String selectPropertyNamme = getParentSelectPropertyName(element);
            if (selectPropertyNamme != null) {
                map.put("th:selected", String.format("${%s.code()} == ${%s}", spec.getIterVarName(), selectPropertyNamme));
            }
        }
        return map;
    }

    protected String getParentSelectPropertyName(Element element) {
        if (element.hasNodeProperty(PropertyAttrProcessor.SELECT_PROPERTY_NAME)) {
            return (String)element.getNodeProperty(PropertyAttrProcessor.SELECT_PROPERTY_NAME);
        }
        if ("select".equals(element.getNormalizedName())) {
            return null;
        }
        return getParentSelectPropertyName((Element)element.getParent());
    }

    /**
     * {@inheritDoc}
     * @see org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor#getModificationType(org.thymeleaf.Arguments, org.thymeleaf.dom.Element, java.lang.String, java.lang.String)
     */
    @Override
    protected ModificationType getModificationType(Arguments arguments, Element element, String attributeName, String newAttributeName) {
        return ModificationType.SUBSTITUTION;
    }

    /**
     * {@inheritDoc}
     * @see org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor#removeAttributeIfEmpty(org.thymeleaf.Arguments, org.thymeleaf.dom.Element, java.lang.String, java.lang.String)
     */
    @Override
    protected boolean removeAttributeIfEmpty(Arguments arguments, Element element, String attributeName, String newAttributeName) {
        return true;
    }

    /**
     * {@inheritDoc}
     * @see org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor#recomputeProcessorsAfterExecution(org.thymeleaf.Arguments, org.thymeleaf.dom.Element, java.lang.String)
     */
    @Override
    protected boolean recomputeProcessorsAfterExecution(Arguments arguments, Element element, String attributeName) {
        return true;
    }

    protected IterationSpec getIterationSpec(final Arguments arguments, final Element element, final String attributeName) {
        final Configuration configuration = arguments.getConfiguration();

        // Obtain the Thymeleaf Standard Expression parser
        final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);

        // Obtain the attribute value
        final String attributeValue = element.getAttributeValue(attributeName);
        int separateIndex = attributeValue.indexOf(":");
        if (separateIndex < 0) {
            // Parse the attribute value as a Thymeleaf Standard Expression
            final IStandardExpression expression = parser.parseExpression(configuration, arguments, attributeValue);

            String classificationName = expression.execute(configuration, arguments).toString();
            return new IterationSpec(DEFAULT_ITERATION_VALUE, DEFAULT_ITERATION_VALUE + AbstractIterationAttrProcessor.DEFAULT_STATUS_VAR_SUFFIX, classificationName);
        }

        String classification = attributeValue.substring(separateIndex + 1).trim();
        String iterVarName = attributeValue.substring(0, separateIndex).trim();
        String statusVarName = null;
        int statusSeparate = iterVarName.indexOf(",");
        if (statusSeparate < 0) {
            statusVarName = iterVarName + AbstractIterationAttrProcessor.DEFAULT_STATUS_VAR_SUFFIX;
        } else {
            statusVarName = iterVarName.substring(statusSeparate + 1).trim();
            iterVarName = iterVarName.substring(0, statusSeparate).trim();
        }
        // Parse the attribute value as a Thymeleaf Standard Expression
        final IStandardExpression expression = parser.parseExpression(configuration, arguments, classification);

        String classificationName = expression.execute(configuration, arguments).toString();

        return new IterationSpec(iterVarName, statusVarName, classificationName);
    }

    // ===================================================================================
    //                                                                     Internal Object
    //                                                                     ===============
    protected static class IterationSpec {

        private final String iterVarName;
        private final String statusVarName;
        private final String classificationName;

        public IterationSpec(final String iterVarName, final String statusVarName, final String classificationName) {
            super();
            Validate.notEmpty(iterVarName, "Iteration var name cannot be null or empty");
            this.iterVarName = iterVarName;
            this.statusVarName = statusVarName;
            this.classificationName = classificationName;
        }

        public String getIterVarName() {
            return this.iterVarName;
        }

        public String getStatusVarName() {
            return this.statusVarName;
        }

        public Object getClassificationName() {
            return this.classificationName;
        }
    }
}
