ALTER TABLE user_profile_attributes
    DROP COLUMN IF EXISTS include_in_userinfo;

ALTER TABLE user_profile_attributes
    DROP COLUMN IF EXISTS include_in_id_token;

ALTER TABLE user_profile_attributes
    DROP COLUMN IF EXISTS include_in_access_token;