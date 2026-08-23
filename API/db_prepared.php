<?php
/**
 * P5-2: Incremental mysqli prepared-statement helpers.
 * Use for new hardening; existing endpoints migrate incrementally.
 */

if (!function_exists('db_stmt_bind_params')) {
    /**
     * @param mysqli_stmt $stmt
     * @param string $types
     * @param array $params
     * @return bool
     */
    function db_stmt_bind_params($stmt, $types, array $params)
    {
        if ($types === '' || empty($params)) {
            return true;
        }
        $bind = array($types);
        foreach ($params as $key => $value) {
            $bind[] = &$params[$key];
        }
        return call_user_func_array(array($stmt, 'bind_param'), $bind);
    }
}

if (!function_exists('db_stmt_fetch_one')) {
    /**
     * @param mysqli $con
     * @param string $sql
     * @param string $types
     * @param mixed ...$params
     * @return array|null
     */
    function db_stmt_fetch_one($con, $sql, $types = '', ...$params)
    {
        $stmt = mysqli_prepare($con, $sql);
        if (!$stmt) {
            return null;
        }
        if ($types !== '' && !db_stmt_bind_params($stmt, $types, $params)) {
            mysqli_stmt_close($stmt);
            return null;
        }
        if (!mysqli_stmt_execute($stmt)) {
            mysqli_stmt_close($stmt);
            return null;
        }
        $result = mysqli_stmt_get_result($stmt);
        $row = ($result !== false) ? mysqli_fetch_assoc($result) : null;
        mysqli_stmt_close($stmt);
        return ($row !== null && $row !== false) ? $row : null;
    }
}

if (!function_exists('db_stmt_fetch_all')) {
    /**
     * @param mysqli $con
     * @param string $sql
     * @param string $types
     * @param mixed ...$params
     * @return array
     */
    function db_stmt_fetch_all($con, $sql, $types = '', ...$params)
    {
        $rows = array();
        $stmt = mysqli_prepare($con, $sql);
        if (!$stmt) {
            return $rows;
        }
        if ($types !== '' && !db_stmt_bind_params($stmt, $types, $params)) {
            mysqli_stmt_close($stmt);
            return $rows;
        }
        if (!mysqli_stmt_execute($stmt)) {
            mysqli_stmt_close($stmt);
            return $rows;
        }
        $result = mysqli_stmt_get_result($stmt);
        if ($result !== false) {
            while ($row = mysqli_fetch_assoc($result)) {
                $rows[] = $row;
            }
        }
        mysqli_stmt_close($stmt);
        return $rows;
    }
}

if (!function_exists('db_stmt_execute')) {
    /**
     * @param mysqli $con
     * @param string $sql
     * @param string $types
     * @param mixed ...$params
     * @return bool
     */
    function db_stmt_execute($con, $sql, $types = '', ...$params)
    {
        $stmt = mysqli_prepare($con, $sql);
        if (!$stmt) {
            return false;
        }
        if ($types !== '' && !db_stmt_bind_params($stmt, $types, $params)) {
            mysqli_stmt_close($stmt);
            return false;
        }
        $ok = mysqli_stmt_execute($stmt);
        mysqli_stmt_close($stmt);
        return (bool) $ok;
    }
}

if (!function_exists('db_stmt_insert_id')) {
    /**
     * @param mysqli $con
     * @param string $sql
     * @param string $types
     * @param mixed ...$params
     * @return int|false insert id on success, false on failure
     */
    function db_stmt_insert_id($con, $sql, $types = '', ...$params)
    {
        $stmt = mysqli_prepare($con, $sql);
        if (!$stmt) {
            return false;
        }
        if ($types !== '' && !db_stmt_bind_params($stmt, $types, $params)) {
            mysqli_stmt_close($stmt);
            return false;
        }
        if (!mysqli_stmt_execute($stmt)) {
            mysqli_stmt_close($stmt);
            return false;
        }
        $id = mysqli_insert_id($con);
        mysqli_stmt_close($stmt);
        return $id;
    }
}

if (!function_exists('db_stmt_scalar_int')) {
    /**
     * @param mysqli $con
     * @param string $sql
     * @param string $types
     * @param mixed ...$params
     * @return int
     */
    function db_stmt_scalar_int($con, $sql, $types = '', ...$params)
    {
        $row = db_stmt_fetch_one($con, $sql, $types, ...$params);
        if ($row === null) {
            return 0;
        }
        $value = reset($row);
        return ($value !== null && $value !== false) ? (int) $value : 0;
    }
}

if (!function_exists('db_stmt_scalar_string')) {
    /**
     * @param mysqli $con
     * @param string $sql
     * @param string $types
     * @param mixed ...$params
     * @return string|null
     */
    function db_stmt_scalar_string($con, $sql, $types = '', ...$params)
    {
        $row = db_stmt_fetch_one($con, $sql, $types, ...$params);
        if ($row === null) {
            return null;
        }
        $value = reset($row);
        return ($value !== null && $value !== false) ? (string) $value : null;
    }
}
