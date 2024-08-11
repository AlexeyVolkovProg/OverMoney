import React, { FC, useContext, useEffect, useState } from 'react';
import { Container } from 'react-bootstrap';
import TelegramLoginButton from '../components/TelegramLoginButton';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import axios from 'axios';
import { IUser } from '../types/types';

const LoginPage: FC = () => {
    const url = process.env.REACT_APP_PATH_TO_HOST || '/front'
    const baseUrl = process.env.REACT_APP_BASE_URL || 'https://overmoney.tech'

    const [botName, setBotName] = useState('')
    const { setAuthenticated } = useContext(AuthContext)

    const navigate = useNavigate()

    useEffect(() => {
        axios.get(`${baseUrl}/login/bot-login`)
            .then(response => {
                setBotName(response.data)
                console.log(response.data)
            })
            .catch(error => console.log(error))
    }, [])
    // Обработка данных пользователя после аутентификации через Telegram Login Widget
    const handleUserAuth = (user: IUser) => {
        console.log(user); 
        axios.post(`${baseUrl}/auth/login`, user)
            .then(response => {
                    console.log("success login")
                    setAuthenticated(true)
                    navigate(`${url}/overmoney`)
                }
            )
            .catch(error => console.log("error login"))
    };
    return (
        <Container className="d-flex align-items-center justify-content-center mt-5">
            <TelegramLoginButton botName={botName} dataOnauth={handleUserAuth} />
            {/* <TelegramLoginButton botName="testSignin_bot" dataOnauth={handleUserAuth} /> */}
        </Container>
    );
};

export default LoginPage;