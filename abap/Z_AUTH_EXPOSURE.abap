*&---------------------------------------------------------------------*
*& Function Module Z_AUTH_EXPOSURE
*&---------------------------------------------------------------------*
*& RFC enabled function module that reads the roles, the authorisation
*& values and the assignments of a system, so the Authorisation & Access
*& Governance Analyzer can run against a live landscape instead of a
*& spreadsheet somebody keeps by hand.
*&
*& CREATED WITH SE37 -> "Remote-Enabled" MUST BE TICKED.
*& Without that flag the function module is not registered for RFC and the
*& call fails with "not found / not remote enabled" - which appears in
*& SM58 on the calling system, not as a dump here.
*&
*& Source: AGR_ROLES - the role master, with the type (single or composite)
*& and the parent role of every composite member.
*&        AGR_AUTH  - the authorisation values each role holds, with the
*&        object, the field and the field value.
*&        AGR_USERS - who holds which role, in which system.
*&
*& A note on honesty: these tables are the classic PFCG tables and they are
*& stable, but the exact column set differs between releases, and the
*& authorisation object catalogue is not in them at all - the sensitivity of
*& an object ("table maintenance is critical, display is not") is a judgement
*& a security team makes, not something the database holds. This module
*& therefore returns the raw grants and leaves the judgement to the
*& analyzer, which is where the policy belongs anyway.
*&
*& What this function module deliberately does NOT do: decide whether two
*& roles conflict. The segregation-of-duties rules are a customer's own
*& policy, and only the analyzer holds them.
*&---------------------------------------------------------------------*

FUNCTION z_auth_exposure.
*"*" Local Interface:
*"*" IMPORTING
*"*" VALUE(iv_system) TYPE string OPTIONAL
*"*" EXPORTING
*"*" VALUE(ev_role_count) TYPE i
*"*" VALUE(ev_user_count) TYPE i
*"*" VALUE(ev_message) TYPE string
*"*" TABLES
*"*" it_roles TYPE zaut_role_tab
*"*" it_users TYPE zaut_user_tab

  DATA: lv_role_count TYPE i,
        lv_user_count TYPE i.

  CLEAR: ev_role_count, ev_user_count, ev_message, it_roles, it_users.

  " -- the roles and the authorisation values they hold -------------------
  SELECT agr_name agr_type parent_role agr_text
         auth_name field name
    FROM agr_auth
    INNER JOIN agr_roles
      ON agr_roles.agr_name = agr_auth.agr_name
    WHERE ( @iv_system IS INITIAL OR agr_auth.agr_name IS NOT INITIAL )
    INTO TABLE @DATA(lt_auth).

  LOOP AT lt_auth INTO DATA(ls_auth).
    lv_role_count = lv_role_count + 1.

    APPEND VALUE #(
      role_name   = ls_auth-agr_name
      description = ls_auth-agr_text
      role_type   = COND string(
                        WHEN ls_auth-agr_type = 'C' THEN 'COMPOSITE'
                        ELSE 'SINGLE' )
      parent_role = ls_auth-parent_role
      owner       = ''
      status      = 'ACTIVE'
      auth_name   = ls_auth-auth_name
      field_name  = ls_auth-field
      field_value = ls_auth-name
      system_id   = iv_system
    ) TO it_roles.
  ENDLOOP.

  " -- who holds what ------------------------------------------------------
  SELECT userid agr_name
    FROM agr_users
    WHERE ( @iv_system IS INITIAL OR agr_users.agr_name IS NOT INITIAL )
    INTO TABLE @DATA(lt_users).

  LOOP AT lt_users INTO DATA(ls_user).
    lv_user_count = lv_user_count + 1.

    APPEND VALUE #(
      user_id   = ls_user-userid
      role_name = ls_user-agr_name
      system_id = iv_system
    ) TO it_users.
  ENDLOOP.

  ev_role_count = lv_role_count.
  ev_user_count = lv_user_count.

  IF lv_role_count = 0.
    ev_message = |No roles found for { iv_system }.|.
  ELSE.
    ev_message = |{ lv_role_count } grant(s) and { lv_user_count } assignment(s) returned.|.
  ENDIF.

ENDFUNCTION.
