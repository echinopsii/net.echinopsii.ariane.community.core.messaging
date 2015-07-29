{% extends "plan_module_parent_tpl.xml" %}
{% block name %}
    {%- if module.type == 'none' -%}
        "net.echinopsii.ariane.community.{{module.name}}"
    {%- else -%}
        "net.echinopsii.ariane.community.core.{{module.name}}"
    {%- endif -%}
{% endblock %}
{% block version %}"{{version}}"{% endblock %}
{%- block configuration -%}{%- endblock -%}
{% block bundle %}
        {% for s in submodules -%}
        {%- if module.type == 'none' %}
        <artifact type="bundle" name="net.echinopsii.ariane.community.{{module.name}}.{{s.name}}" version="[{{vmin}},{{vmax}})"/>
        {%- else -%}
        <artifact type="bundle" name="net.echinopsii.ariane.community.core.{{module.name}}.{{s.name}}" version="[{{vmin}},{{vmax}})"/>
        {% endif -%}
        {%- endfor %}
{% endblock %}
