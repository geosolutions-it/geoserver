.. _community_keycloak:

Keycloak extension
==================

The ``Keycloak module`` allows GeoServer to work with a `keycloak service <https://www.keycloak.org/>`__ to authenticate users and establish authorization using roles.

   The :guilabel:`Enable redirect to Keycloak Login page` checkbox should be checked if the desired behaviour is to authenticate on the web ui only through keycloak. Otherwise if the keycloak filter needs to coexists with other filter on the ``/web`` filter chain it must be unchecked. In this case we will keep it checked.

   The :guilabel:`Role Source` drop down enable the selection of the desired role source for the user being authenticated through keycloak. If none is selected by default the ``Active Role Service`` will be used.


.. toctree::
   :maxdepth: 2

   installation
   authentication
   keycloak_role_service
