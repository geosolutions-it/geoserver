.. _security_tutorials_oauth2:

Authentication with OAuth2
==========================

This tutorial introduces GeoServer OAuth2 support and walks through the process of
setting up authentication against an OAuth2 provider. It is recommended that the 
:ref:`security_auth_chain` section be read before proceeding.

.. toctree::
   :maxdepth: 1

   installing
   google
   oauth2
   oidc
The UI allow to set also the ``Post Logout Redirect URI`` which will be used to populate the  ``post_logout_redirect_uri`` request param, when doing the global logout from the GeoServer UI. The OpenId provider will use the URI to redirect to the desired app page.


Finally the admin can allow the sending of the client_secret during an access_token request trough the ``Send Client Secret in Token Request``. Some OpenId implementation requires it for the Authorization Code flow when the client app is a confidential client and can safely store the client_secret.

Azure AD and ADFS setup
^^^^^^^^^^^^^^^^^^^^^^^
To make the OpenIdConnect filter to work properly with an Azure AD or ADFS server via the OpenId protocol, the user must set, in addition to the other configuration parameters, the ``Response Mode`` to query (otherwise by default ADFS will return a url fragment) and check the checkbox ``Send Client Secret in Token Request`` (the client_secret is mandatory in token request according to the `Microsoft documentation <https://docs.microsoft.com/en-us/windows-server/identity/ad-fs/overview/ad-fs-openid-connect-oauth-flows-scenarios#request-an-access-token>`_).

