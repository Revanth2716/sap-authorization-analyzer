*&---------------------------------------------------------------------*
*& DDIC types for the Authorisation & Access Governance RFC interface
*&---------------------------------------------------------------------*
*& Create these in SE11. Flat structures, so they can be used as RFC
*& table parameters without any conversion rules.
*&---------------------------------------------------------------------*

"! <p class="text">One role with the authorisation values it grants.</p>
TYPES : BEGIN OF zaut_role,
         role_name   TYPE string,   " The role
         description TYPE string,   " Short text
         role_type   TYPE string,   " SINGLE / COMPOSITE
         parent_role TYPE string,   " The composite holding it
         owner       TYPE string,   " Who owns it
         status      TYPE string,   " ACTIVE / INACTIVE
         auth_name   TYPE string,   " The authorisation, e.g. F_BKPF_BUK
         field_name  TYPE string,   " The field, e.g. ACTVT
         field_value TYPE string,   " The value, e.g. '*'
         system_id   TYPE string,   " The system the grant belongs to
       END OF zaut_role.

TYPES zaut_role_tab TYPE STANDARD TABLE OF zaut_role.

"! <p class="text">One user holding one role.</p>
TYPES : BEGIN OF zaut_user,
         user_id   TYPE string,   " The user
         role_name TYPE string,   " The role they hold
         system_id TYPE string,   " The system
       END OF zaut_user.

TYPES zaut_user_tab TYPE STANDARD TABLE OF zaut_user.
